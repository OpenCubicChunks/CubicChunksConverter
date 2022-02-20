/*
 *  This file is part of CubicChunksConverter, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2017-2021 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.converter.lib.util;

public class Matrix4d {

    public double m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33;

    public Matrix4d() {
        setIdentity();
    }

    public Matrix4d(double m00, double m01, double m02, double m03,
                    double m10, double m11, double m12, double m13,
                    double m20, double m21, double m22, double m23,
                    double m30, double m31, double m32, double m33) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(m00).append(' ').append(m10).append(' ').append(m20).append(' ').append(m30).append('\n');
        buf.append(m01).append(' ').append(m11).append(' ').append(m21).append(' ').append(m31).append('\n');
        buf.append(m02).append(' ').append(m12).append(' ').append(m22).append(' ').append(m32).append('\n');
        buf.append(m03).append(' ').append(m13).append(' ').append(m23).append(' ').append(m33).append('\n');
        return buf.toString();
    }

    public Matrix4d setIdentity() {
        this.m00 = 1;
        this.m01 = 0;
        this.m02 = 0;
        this.m03 = 0;
        this.m10 = 0;
        this.m11 = 1;
        this.m12 = 0;
        this.m13 = 0;
        this.m20 = 0;
        this.m21 = 0;
        this.m22 = 1;
        this.m23 = 0;
        this.m30 = 0;
        this.m31 = 0;
        this.m32 = 0;
        this.m33 = 1;
        return this;
    }

    public Matrix4d load(Matrix4d src) {
        this.m00 = src.m00;
        this.m01 = src.m01;
        this.m02 = src.m02;
        this.m03 = src.m03;
        this.m10 = src.m10;
        this.m11 = src.m11;
        this.m12 = src.m12;
        this.m13 = src.m13;
        this.m20 = src.m20;
        this.m21 = src.m21;
        this.m22 = src.m22;
        this.m23 = src.m23;
        this.m30 = src.m30;
        this.m31 = src.m31;
        this.m32 = src.m32;
        this.m33 = src.m33;
        return this;
    }

    public static Matrix4d add(Matrix4d left, Matrix4d right, Matrix4d dest) {
        if (dest == null) {
            dest = new Matrix4d();
        }

        dest.m00 = left.m00 + right.m00;
        dest.m01 = left.m01 + right.m01;
        dest.m02 = left.m02 + right.m02;
        dest.m03 = left.m03 + right.m03;
        dest.m10 = left.m10 + right.m10;
        dest.m11 = left.m11 + right.m11;
        dest.m12 = left.m12 + right.m12;
        dest.m13 = left.m13 + right.m13;
        dest.m20 = left.m20 + right.m20;
        dest.m21 = left.m21 + right.m21;
        dest.m22 = left.m22 + right.m22;
        dest.m23 = left.m23 + right.m23;
        dest.m30 = left.m30 + right.m30;
        dest.m31 = left.m31 + right.m31;
        dest.m32 = left.m32 + right.m32;
        dest.m33 = left.m33 + right.m33;

        return dest;
    }

    public static Matrix4d sub(Matrix4d left, Matrix4d right, Matrix4d dest) {
        if (dest == null) {
            dest = new Matrix4d();
        }

        dest.m00 = left.m00 - right.m00;
        dest.m01 = left.m01 - right.m01;
        dest.m02 = left.m02 - right.m02;
        dest.m03 = left.m03 - right.m03;
        dest.m10 = left.m10 - right.m10;
        dest.m11 = left.m11 - right.m11;
        dest.m12 = left.m12 - right.m12;
        dest.m13 = left.m13 - right.m13;
        dest.m20 = left.m20 - right.m20;
        dest.m21 = left.m21 - right.m21;
        dest.m22 = left.m22 - right.m22;
        dest.m23 = left.m23 - right.m23;
        dest.m30 = left.m30 - right.m30;
        dest.m31 = left.m31 - right.m31;
        dest.m32 = left.m32 - right.m32;
        dest.m33 = left.m33 - right.m33;

        return dest;
    }

    public static Matrix4d mul(Matrix4d left, Matrix4d right, Matrix4d dest) {
        if (dest == null) {
            dest = new Matrix4d();
        }

        double m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02 + left.m30 * right.m03;
        double m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02 + left.m31 * right.m03;
        double m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02 + left.m32 * right.m03;
        double m03 = left.m03 * right.m00 + left.m13 * right.m01 + left.m23 * right.m02 + left.m33 * right.m03;
        double m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12 + left.m30 * right.m13;
        double m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12 + left.m31 * right.m13;
        double m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12 + left.m32 * right.m13;
        double m13 = left.m03 * right.m10 + left.m13 * right.m11 + left.m23 * right.m12 + left.m33 * right.m13;
        double m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22 + left.m30 * right.m23;
        double m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22 + left.m31 * right.m23;
        double m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22 + left.m32 * right.m23;
        double m23 = left.m03 * right.m20 + left.m13 * right.m21 + left.m23 * right.m22 + left.m33 * right.m23;
        double m30 = left.m00 * right.m30 + left.m10 * right.m31 + left.m20 * right.m32 + left.m30 * right.m33;
        double m31 = left.m01 * right.m30 + left.m11 * right.m31 + left.m21 * right.m32 + left.m31 * right.m33;
        double m32 = left.m02 * right.m30 + left.m12 * right.m31 + left.m22 * right.m32 + left.m32 * right.m33;
        double m33 = left.m03 * right.m30 + left.m13 * right.m31 + left.m23 * right.m32 + left.m33 * right.m33;

        dest.m00 = m00;
        dest.m01 = m01;
        dest.m02 = m02;
        dest.m03 = m03;
        dest.m10 = m10;
        dest.m11 = m11;
        dest.m12 = m12;
        dest.m13 = m13;
        dest.m20 = m20;
        dest.m21 = m21;
        dest.m22 = m22;
        dest.m23 = m23;
        dest.m30 = m30;
        dest.m31 = m31;
        dest.m32 = m32;
        dest.m33 = m33;

        return dest;
    }

    public int transformX(int vecX, int vecY, int vecZ) {
        return (int) Math.round(this.m00 * vecX + this.m10 * vecY + this.m20 * vecZ + this.m30);
    }

    public int transformY(int vecX, int vecY, int vecZ) {
        return (int) Math.round(this.m01 * vecX + this.m11 * vecY + this.m21 * vecZ + this.m31);
    }

    public int transformZ(int vecX, int vecY, int vecZ) {
        return (int) Math.round(this.m02 * vecX + this.m12 * vecY + this.m22 * vecZ + this.m32);
    }

    public Vector3i transformVec3i(Vector3i vec) {
        double x = this.m00 * vec.getX() + this.m10 * vec.getY() + this.m20 * vec.getZ() + this.m30;
        double y = this.m01 * vec.getX() + this.m11 * vec.getY() + this.m21 * vec.getZ() + this.m31;
        double z = this.m02 * vec.getX() + this.m12 * vec.getY() + this.m22 * vec.getZ() + this.m32;

        return new Vector3i((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
    }

    public Matrix4d translate(Vector3i vec) {
        this.m30 += this.m00 * vec.getX() + this.m10 * vec.getY() + this.m20 * vec.getZ();
        this.m31 += this.m01 * vec.getX() + this.m11 * vec.getY() + this.m21 * vec.getZ();
        this.m32 += this.m02 * vec.getX() + this.m12 * vec.getY() + this.m22 * vec.getZ();
        this.m33 += this.m03 * vec.getX() + this.m13 * vec.getY() + this.m23 * vec.getZ();
        return this;
    }

    public double determinant() {
        double det = m00 * ((m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32)
                - m13 * m22 * m31 - m11 * m23 * m32 - m12 * m21 * m33);
        det -= m01 * ((m10 * m22 * m33 + m12 * m23 * m30 + m13 * m20 * m32)
                - m13 * m22 * m30 - m10 * m23 * m32 - m12 * m20 * m33);
        det += m02 * ((m10 * m21 * m33 + m11 * m23 * m30 + m13 * m20 * m31)
                - m13 * m21 * m30 - m10 * m23 * m31 - m11 * m20 * m33);
        det -= m03 * ((m10 * m21 * m32 + m11 * m22 * m30 + m12 * m20 * m31)
                - m12 * m21 * m30 - m10 * m22 * m31 - m11 * m20 * m32);
        return det;
    }

    private static double det33(
            double m00, double m01, double m02,
            double m10, double m11, double m12,
            double m20, double m21, double m22) {
        return m00 * (m11 * m22 - m12 * m21)
                + m01 * (m12 * m20 - m10 * m22)
                + m02 * (m10 * m21 - m11 * m20);
    }

    public Matrix4d inverse() {
        double determinant = determinant();

        if (determinant == 0) {
            throw new IllegalStateException("Non-invertible matrix");
        }
        double detInv = 1.0 / determinant;


        double t00 = det33(m11, m12, m13, m21, m22, m23, m31, m32, m33);
        double t01 = -det33(m10, m12, m13, m20, m22, m23, m30, m32, m33);
        double t02 = det33(m10, m11, m13, m20, m21, m23, m30, m31, m33);
        double t03 = -det33(m10, m11, m12, m20, m21, m22, m30, m31, m32);

        double t10 = -det33(m01, m02, m03, m21, m22, m23, m31, m32, m33);
        double t11 = det33(m00, m02, m03, m20, m22, m23, m30, m32, m33);
        double t12 = -det33(m00, m01, m03, m20, m21, m23, m30, m31, m33);
        double t13 = det33(m00, m01, m02, m20, m21, m22, m30, m31, m32);

        double t20 = det33(m01, m02, m03, m11, m12, m13, m31, m32, m33);
        double t21 = -det33(m00, m02, m03, m10, m12, m13, m30, m32, m33);
        double t22 = det33(m00, m01, m03, m10, m11, m13, m30, m31, m33);
        double t23 = -det33(m00, m01, m02, m10, m11, m12, m30, m31, m32);

        double t30 = -det33(m01, m02, m03, m11, m12, m13, m21, m22, m23);
        double t31 = det33(m00, m02, m03, m10, m12, m13, m20, m22, m23);
        double t32 = -det33(m00, m01, m03, m10, m11, m13, m20, m21, m23);
        double t33 = det33(m00, m01, m02, m10, m11, m12, m20, m21, m22);

        Matrix4d dest = new Matrix4d();
        dest.m00 = t00 * detInv;
        dest.m11 = t11 * detInv;
        dest.m22 = t22 * detInv;
        dest.m33 = t33 * detInv;
        dest.m01 = t10 * detInv;
        dest.m10 = t01 * detInv;
        dest.m20 = t02 * detInv;
        dest.m02 = t20 * detInv;
        dest.m12 = t21 * detInv;
        dest.m21 = t12 * detInv;
        dest.m03 = t30 * detInv;
        dest.m30 = t03 * detInv;
        dest.m13 = t31 * detInv;
        dest.m31 = t13 * detInv;
        dest.m32 = t23 * detInv;
        dest.m23 = t32 * detInv;
        return dest;
    }
}
