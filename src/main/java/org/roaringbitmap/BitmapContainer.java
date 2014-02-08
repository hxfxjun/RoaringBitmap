package org.roaringbitmap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

public final class BitmapContainer extends Container implements Cloneable,
        Serializable {

        /**
         * Create a bitmap container with all bits set to false
         */
        public BitmapContainer() {
                this.cardinality = 0;
        }

        @Override
        public Container add(final short i) {
                final int x = Util.toIntUnsigned(i);
                final long previous = bitmap[x / 64];
                bitmap[x / 64] |= (1l << x);
                cardinality += (previous ^ bitmap[x / 64]) >>> x;
                // if(previous != (bitmap[x/64] |= (1l << x)) )
                // ++cardinality;
                return this;
        }

        @Override
        public ArrayContainer and(final ArrayContainer value2) {
                final ArrayContainer answer = ContainerFactory
                        .getArrayContainer();
                if (answer.content.length < value2.content.length)
                        answer.content = new short[value2.content.length];
                for (int k = 0; k < value2.getCardinality(); ++k)
                        if (this.contains(value2.content[k]))
                                answer.content[answer.cardinality++] = value2.content[k];
                return answer;
        }

        @Override
        public Container and(final BitmapContainer value2) {
                final BitmapContainer answer = ContainerFactory
                        .getUnintializedBitmapContainer();
                answer.cardinality = 0;
                for (int k = 0; k < answer.bitmap.length; ++k) {
                        answer.bitmap[k] = this.bitmap[k] & value2.bitmap[k];
                        answer.cardinality += Long.bitCount(answer.bitmap[k]);
                }
                if (answer.cardinality <= ArrayContainer.DEFAULTMAXSIZE)
                        return ContainerFactory
                                .transformToArrayContainer(answer);
                return answer;
        }

        @Override
        public Container andNot(final ArrayContainer value2) {
                final BitmapContainer answer = ContainerFactory
                        .getCopyOfBitmapContainer(this);
                for (int k = 0; k < value2.cardinality; ++k) {
                        final int i = Util.toIntUnsigned(value2.content[k]) >>> 6;
                        answer.bitmap[i] = answer.bitmap[i]
                                & (~(1l << value2.content[k]));
                        answer.cardinality -= (answer.bitmap[i] ^ this.bitmap[i]) >>> value2.content[k];// subtract
                                                                                                        // one
                                                                                                        // if
                                                                                                        // they
                                                                                                        // differ
                }
                if (answer.cardinality <= ArrayContainer.DEFAULTMAXSIZE)
                        return ContainerFactory
                                .transformToArrayContainer(answer);
                return answer;
        }

        @Override
        public Container andNot(final BitmapContainer value2) {
                final BitmapContainer answer = ContainerFactory
                        .getUnintializedBitmapContainer();
                answer.cardinality = 0;

                for (int k = 0; k < answer.bitmap.length; ++k) {
                        answer.bitmap[k] = this.bitmap[k] & (~value2.bitmap[k]);
                        answer.cardinality += Long.bitCount(answer.bitmap[k]);

                }
                if (answer.cardinality <= ArrayContainer.DEFAULTMAXSIZE)
                        return ContainerFactory
                                .transformToArrayContainer(answer);
                return answer;
        }

        @Override
        public void clear() {
                if (cardinality != 0) {
                        cardinality = 0;
                        Arrays.fill(bitmap, 0);
                }
        }

        @Override
        public BitmapContainer clone() {
                final BitmapContainer x = (BitmapContainer) super.clone();
                x.cardinality = this.cardinality;
                x.bitmap = Arrays.copyOf(this.bitmap, this.bitmap.length);
                return x;
        }

        @Override
        public boolean contains(final short i) {
                final int x = Util.toIntUnsigned(i);
                return (bitmap[x / 64] & (1l << x)) != 0;
        }

        @Override
        public boolean equals(Object o) {
                if (o instanceof BitmapContainer) {
                        BitmapContainer srb = (BitmapContainer) o;
                        if (srb.cardinality != this.cardinality)
                                return false;
                        return Arrays.equals(this.bitmap, srb.bitmap);
                }
                return false;
        }

        @Override
        public int getCardinality() {
                return cardinality;
        }

        @Override
        public ShortIterator getShortIterator() {
                return new ShortIterator() {
                        @Override
                        public boolean hasNext() {
                                return i >= 0;
                        }

                        @Override
                        public short next() {
                                short j = (short) i;
                                i = BitmapContainer.this.nextSetBit(i + 1);
                                return j;
                        }

                        int i = BitmapContainer.this.nextSetBit(0);

                };

        }

        @Override
        public int getSizeInBits() {
                // the standard size is DEFAULTMAXSIZE chunks * 64bits
                // each=65536 bits,
                // each 1 bit represents an integer from 0 to 65535
                return 65536 + 32;
        }

        @Override
        public int getSizeInBytes() {
                return this.bitmap.length * 8;
        }

        @Override
        public Container iand(final ArrayContainer B2) {
                return B2.and(this);// no inplace possible
        }

        @Override
        public Container iand(final BitmapContainer B2) {
                this.cardinality = 0;
                for (int k = 0; k < this.bitmap.length; k++) {
                        this.bitmap[k] &= B2.bitmap[k];
                        this.cardinality += Long.bitCount(this.bitmap[k]);
                }
                return this;
        }

        @Override
        public Container iandNot(final ArrayContainer B2) {
                for (int k = 0; k < B2.cardinality; ++k) {
                        this.remove(B2.content[k]);
                }
                if (cardinality <= ArrayContainer.DEFAULTMAXSIZE)
                        return ContainerFactory.transformToArrayContainer(this);
                return this;
        }

        @Override
        public Container iandNot(final BitmapContainer B2) {
                this.cardinality = 0;
                for (int k = 0; k < this.bitmap.length; k++) {
                        this.bitmap[k] &= ~B2.bitmap[k];
                        this.cardinality += Long.bitCount(this.bitmap[k]);
                }
                if (cardinality <= ArrayContainer.DEFAULTMAXSIZE)
                        return ContainerFactory.transformToArrayContainer(this);
                return this;
        }

        @Override
        public BitmapContainer ior(final ArrayContainer value2) {
                for (int k = 0; k < value2.cardinality; ++k) {
                        final int i = Util.toIntUnsigned(value2.content[k]) >>> 6;
                        this.cardinality += ((~this.bitmap[i]) & (1l << value2.content[k])) >>> value2.content[k];// in
                                                                                                                  // Java,
                                                                                                                  // shifts
                                                                                                                  // are
                                                                                                                  // always
                                                                                                                  // "modulo"
                        this.bitmap[i] |= (1l << value2.content[k]);
                }
                return this;
        }

        @Override
        public Container ior(final BitmapContainer B2) {
                this.cardinality = 0;
                for (int k = 0; k < this.bitmap.length; k++) {
                        this.bitmap[k] |= B2.bitmap[k];
                        this.cardinality += Long.bitCount(this.bitmap[k]);
                }
                return this;
        }

        @Override
        public Iterator<Short> iterator() {
                return new Iterator<Short>() {
                        @Override
                        public boolean hasNext() {
                                return i >= 0;
                        }

                        @Override
                        public Short next() {
                                j = i;
                                i = BitmapContainer.this.nextSetBit(i + 1);
                                return new Short((short) j);
                        }

                        @Override
                        public void remove() {
                                BitmapContainer.this.remove((short) j);
                        }

                        int i = BitmapContainer.this.nextSetBit(0);

                        int j;

                };
        }

        @Override
        public Container ixor(final ArrayContainer value2) {
                for (int k = 0; k < value2.getCardinality(); ++k) {
                        final int index = Util.toIntUnsigned(value2.content[k]) >>> 6;
                        this.cardinality += 1 - 2 * ((this.bitmap[index] & (1l << value2.content[k])) >>> value2.content[k]);
                        this.bitmap[index] ^= (1l << value2.content[k]);
                }
                if (this.cardinality <= ArrayContainer.DEFAULTMAXSIZE)
                        return ContainerFactory.transformToArrayContainer(this);

                return this;
        }

        @Override
        public Container ixor(BitmapContainer B2) {
                this.cardinality = 0;
                for (int k = 0; k < this.bitmap.length; ++k) {
                        this.bitmap[k] ^= B2.bitmap[k];
                        this.cardinality += Long.bitCount(this.bitmap[k]);
                }
                if (this.cardinality <= ArrayContainer.DEFAULTMAXSIZE)
                        return ContainerFactory.transformToArrayContainer(this);
                return this;
        }

        public void loadData(final ArrayContainer arrayContainer) {
                this.cardinality = arrayContainer.cardinality;

                for (int k = 0; k < arrayContainer.cardinality; ++k) {
                        final short x = arrayContainer.content[k];
                        bitmap[Util.toIntUnsigned(x) / 64] |= (1l << x);
                }
        }

        public int nextSetBit(final int i) {
                int x = i / 64;
                if (x >= bitmap.length)
                        return -1;
                long w = bitmap[x];
                w >>>= i;
                if (w != 0) {
                        return i + Long.numberOfTrailingZeros(w);
                }
                ++x;
                for (; x < bitmap.length; ++x) {
                        if (bitmap[x] != 0) {
                                return x * 64
                                        + Long.numberOfTrailingZeros(bitmap[x]);
                        }
                }
                return -1;
        }

        public short nextUnsetBit(final int i) {
                int x = i / 64;
                long w = ~bitmap[x];
                w >>>= i;
                if (w != 0) {
                        return (short) (i + Long.numberOfTrailingZeros(w));
                }
                ++x;
                for (; x < bitmap.length; ++x) {
                        if (bitmap[x] != ~0) {
                                return (short) (x * 64 + Long
                                        .numberOfTrailingZeros(~bitmap[x]));
                        }
                }
                return -1;
        }

        @Override
        public BitmapContainer or(final ArrayContainer value2) {
                final BitmapContainer answer = ContainerFactory
                        .getCopyOfBitmapContainer(this);
                for (int k = 0; k < value2.cardinality; ++k) {
                        final int i = Util.toIntUnsigned(value2.content[k]) >>> 6;
                        answer.cardinality += ((~answer.bitmap[i]) & (1l << value2.content[k])) >>> value2.content[k];// in
                                                                                                                      // Java,
                                                                                                                      // shifts
                                                                                                                      // are
                                                                                                                      // always
                                                                                                                      // "modulo"
                        answer.bitmap[i] = answer.bitmap[i]
                                | (1l << value2.content[k]);
                }
                return answer;
        }

        @Override
        public Container or(final BitmapContainer value2) {
                final BitmapContainer answer = ContainerFactory
                        .getUnintializedBitmapContainer();
                answer.cardinality = 0;
                for (int k = 0; k < answer.bitmap.length; ++k) {
                        answer.bitmap[k] = this.bitmap[k] | value2.bitmap[k];
                        // if(answer.bitmap[k]!=0) //DL: I am not sure that the
                        // branching helps here. Would need to benchmark this.
                        answer.cardinality += Long.bitCount(answer.bitmap[k]);
                }
                return answer;
        }

        @Override
        public Container remove(final short i) {
                final int x = Util.toIntUnsigned(i);
                if (cardinality == ArrayContainer.DEFAULTMAXSIZE) {// this is
                                                                   // the
                                                                   // uncommon
                                                                   // path
                        if ((bitmap[x / 64] & (1l << x)) != 0) {
                                --cardinality;
                                bitmap[x / 64] &= ~(1l << x);
                                return ContainerFactory
                                        .transformToArrayContainer(this);
                        }
                }
                cardinality -= (bitmap[x / 64] & (1l << x)) >>> x;
                bitmap[x / 64] &= ~(1l << x);
                return this;
        }

        @Override
        public String toString() {
                StringBuffer sb = new StringBuffer();
                int counter = 0;
                sb.append("{");
                int i = this.nextSetBit(0);
                while (i >= 0) {
                        sb.append(i);
                        ++counter;
                        i = this.nextSetBit(i + 1);
                        if (i >= 0)
                                sb.append(",");
                }
                sb.append("}");
                System.out.println("cardinality = " + cardinality + " "
                        + counter);
                return sb.toString();
        }

        @Override
        public void trim() {
        }

        @Override
        public Container xor(final ArrayContainer value2) {
                final BitmapContainer answer = ContainerFactory
                        .getCopyOfBitmapContainer(this);

                for (int k = 0; k < value2.getCardinality(); ++k) {
                        final int index = Util.toIntUnsigned(value2.content[k]) >>> 6;
                        answer.cardinality += 1 - 2 * ((answer.bitmap[index] & (1l << value2.content[k])) >>> value2.content[k]);
                        answer.bitmap[index] = answer.bitmap[index]
                                ^ (1l << value2.content[k]);

                }
                if (answer.cardinality <= ArrayContainer.DEFAULTMAXSIZE)
                        return ContainerFactory
                                .transformToArrayContainer(answer);
                return answer;
        }

        @Override
        public Container xor(BitmapContainer value2) {
                final BitmapContainer answer = ContainerFactory
                        .getUnintializedBitmapContainer();
                answer.cardinality = 0;
                for (int k = 0; k < answer.bitmap.length; ++k) {
                        answer.bitmap[k] = this.bitmap[k] ^ value2.bitmap[k];
                        answer.cardinality += Long.bitCount(answer.bitmap[k]);
                }
                if (answer.cardinality <= ArrayContainer.DEFAULTMAXSIZE)
                        return ContainerFactory
                                .transformToArrayContainer(answer);
                return answer;
        }

        long[] bitmap = new long[(1 << 16) / 64]; // a max of 65535 integers

        int cardinality;

        private static final long serialVersionUID = 2L;

}