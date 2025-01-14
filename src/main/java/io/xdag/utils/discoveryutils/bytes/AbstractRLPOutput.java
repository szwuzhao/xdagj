/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2030 The XdagJ Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.xdag.utils.discoveryutils.bytes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static io.xdag.utils.discoveryutils.bytes.RLPEncodingHelpers.*;

abstract class AbstractRLPOutput implements RLPOutput {
    /*
     * The algorithm implemented works as follows:
     *
     * Values written to the output are accumulated in the 'values' list. When a list is started, it
     * is indicated by adding a specific marker in that list (LIST_MARKER).
     * While this is gathered, we also incrementally compute the size of the payload of every list of
     * that output. Those sizes are stored in 'payloadSizes': when all the output has been added,
     * payloadSizes[i] will contain the size of the (encoded) payload of the ith list in 'values'
     * (that is, the list that starts at the ith LIST_MARKER in 'values').
     *
     * With that information gathered, encoded() can write its output in a single walk of 'values':
     * values can encoded directly, and every time we read a list marker, we use the corresponding
     * payload size to write the proper prefix and continue.
     *
     * The main remaining aspect is how the values of 'payloadSizes' are computed. Computing the size
     * of a list without nesting inside is easy: simply add the encoded size of any newly added value
     * to the running size. The difficulty is with nesting: when we start a new list, we need to
     * track both the sizes of the previous list and the new one. To deal with that, we use the small
     * stack 'parentListStack': it stores the index in 'payloadSizes' of every currently "open" lists.
     * In other words, payloadSises[parentListStack[stackSize - 1]] corresponds to the size of the
     * current list, the one to which newly added value are currently written (until the next call
     * to 'endList()' that is, while payloadSises[parentListStack[stackSize - 2]] would be the size
     * of the parent list, ....
     *
     * Note that when a new value is added, we add its size only the currently running list. We should
     * add that size to that of any parent list as well, but we do so indirectly when a list is
     * finished: when 'endList()' is called, we add the size of the full list we just finished (and
     * whose size we have now have completely) to its parent size.
     *
     * Side-note: this class internally and informally use "element" to refer to a non list items.
     */

    private static final BytesValue LIST_MARKER = BytesValue.wrap(new byte[0]);

    private final List<BytesValue> values = new ArrayList<>();
    // For every value i in values, rlpEncoded.get(i) will be true only if the value stored is an
    // already encoded item.
    private final BitSet rlpEncoded = new BitSet();

    // First element is the total size of everything (the encoding may be a single non-list item, so
    // this handle that case more easily; we need that value to size out final output). Following
    // elements holds the size of the payload of the ith list in 'values'.
    private int[] payloadSizes = new int[8];
    private int listsCount = 1; // number of lists current in 'values' + 1.

    private int[] parentListStack = new int[4];
    private int stackSize = 1;

    private int currentList() {
        return parentListStack[stackSize - 1];
    }

    @Override
    public void writeBytesValue(final BytesValue v) {
        checkState(
                stackSize > 1 || values.isEmpty(), "Terminated RLP output, cannot add more elements");
        values.add(v);
        payloadSizes[currentList()] += elementSize(v);
    }

    @Override
    public void startList() {
        values.add(LIST_MARKER);
        ++listsCount; // we'll add a new element to payloadSizes
        ++stackSize; // and to the list stack.

        // Resize our lists if necessary.
        if (listsCount > payloadSizes.length) {
            payloadSizes = Arrays.copyOf(payloadSizes, (payloadSizes.length * 3) / 2);
        }
        if (stackSize > parentListStack.length) {
            parentListStack = Arrays.copyOf(parentListStack, (parentListStack.length * 3) / 2);
        }

        // The new current list size is store in the slot we just made room for by incrementing
        // listsCount
        parentListStack[stackSize - 1] = listsCount - 1;
    }

    @Override
    public void endList() {
        checkState(stackSize > 1, "LeaveList() called with no prior matching startList()");

        final int current = currentList();
        final int finishedListSize = listSize(payloadSizes[current]);
        --stackSize;

        // We just finished an item of our parent list, add it to that parent list size now.
        final int newCurrent = currentList();
        payloadSizes[newCurrent] += finishedListSize;
    }

    /**
     * Computes the final encoded data size.
     *
     * @return The size of the RLP-encoded data written to this output.
     * @throws IllegalStateException if some opened list haven't been closed (the output is not valid
     *     as is).
     */
    public int encodedSize() {
        checkState(stackSize == 1, "A list has been entered (startList()) but not left (endList())");
        return payloadSizes[0];
    }

    /**
     * Write the rlp encoded value to the provided {@link MutableBytesValue}
     *
     * @param mutableBytesValue the value to which the rlp-data will be written
     */
    public void writeEncoded(final MutableBytesValue mutableBytesValue) {
        // Special case where we encode only a single non-list item (note that listsCount is initially
        // set to 1, so listsCount == 1 really mean no list explicitly added to the output).
        if (listsCount == 1) {
            // writeBytesValue make sure we cannot have more than 1 value without a list
            assert values.size() == 1;
            final BytesValue value = values.get(0);

            final int finalOffset;
            // Single non-list value.
            if (rlpEncoded.get(0)) {
                value.copyTo(mutableBytesValue, 0);
                finalOffset = value.size();
            } else {
                finalOffset = writeElement(value, mutableBytesValue, 0);
            }
            checkState(
                    finalOffset == mutableBytesValue.size(),
                    "Expected single element RLP encode to be of size %s but was of size %s.",
                    mutableBytesValue.size(),
                    finalOffset);
            return;
        }

        int offset = 0;
        int listIdx = 0;
        for (int i = 0; i < values.size(); i++) {
            final BytesValue value = values.get(i);
            if (value == LIST_MARKER) {
                final int payloadSize = payloadSizes[++listIdx];
                offset = writeListHeader(payloadSize, mutableBytesValue, offset);
            } else if (rlpEncoded.get(i)) {
                value.copyTo(mutableBytesValue, offset);
                offset += value.size();
            } else {
                offset = writeElement(value, mutableBytesValue, offset);
            }
        }

        checkState(
                offset == mutableBytesValue.size(),
                "Expected RLP encoding to be of size %s but was of size %s.",
                mutableBytesValue.size(),
                offset);
    }
}
