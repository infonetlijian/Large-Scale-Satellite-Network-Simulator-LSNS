package satellite_orbit;

/*
import jat.coreNOSA.math.MatrixVector.data.Matrix;
import jat.coreNOSA.math.MatrixVector.data.VectorN;
import jat.coreNOSA.math.MatrixVector.data.arrayTools.Find;
import jat.coreNOSA.math.MatrixVector.data.arrayTools.Sort;
import jat.coreNOSA.math.MatrixVector.data.function.DoubleFunction;
import jat.coreNOSA.math.MatrixVector.data.io.data.MatrixFile;
import jat.coreNOSA.math.MatrixVector.data.io.data.fileTools.MatrixString;
import jat.coreNOSA.math.MatrixVector.data.io.gui.FrameView;
import jat.coreNOSA.math.MatrixVector.data.io.gui.MatrixPlot2D;
import jat.coreNOSA.math.MatrixVector.data.io.gui.MatrixPlot3D;
import jat.coreNOSA.math.MatrixVector.data.io.gui.MatrixTable;
import jat.coreNOSA.math.MatrixVector.data.matrixDecompositions.CholeskyDecomposition;
import jat.coreNOSA.math.MatrixVector.data.matrixDecompositions.EigenvalueDecomposition;
import jat.coreNOSA.math.MatrixVector.data.matrixDecompositions.LUDecomposition;
import jat.coreNOSA.math.MatrixVector.data.matrixDecompositions.Mathfun;
import jat.coreNOSA.math.MatrixVector.data.matrixDecompositions.QRDecomposition;
import jat.coreNOSA.math.MatrixVector.data.matrixDecompositions.SingularValueDecomposition;
*/
import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;

/**
 * <P>
 * The Matrix Class provides the fundamental operations of numerical linear algebra (from the package JAMA),
 * basic manipulations, and visualization tools.
 * All the operations in this version of the Matrix Class involve only real matrices.
 * This Matrix Class is an extension of JMAT's Matrix Class, which was an extension of JAMA's Matrix Class.
 *
 * @author 
 * @version 1.0
 */

public class Matrix implements Cloneable, Serializable {
    
/* ------------------------
   Class variables
 * ------------------------ */
    
    /**
	 * 
	 */
	private static final long serialVersionUID = -5889173925188282718L;

	/** Array for internal storage of elements.
     * @serial internal array storage.
     */
    public double[][] A;
    
    /** Row and column dimensions.
     * @serial row dimension.
     * @serial column dimension.
     */
    public int m, n;
    
/* ------------------------
   Constructors
 * ------------------------ */
    
    /** Construct an m-by-n matrix of zeros.
     * @param m    Number of rows.
     * @param n    Number of colums.
     */
    
    public Matrix(int m, int n) {
        this.m = m;
        this.n = n;
        A = new double[m][n];
    }
    
    /** Construct an n-by-n identity matrix.
     * @param n    Number of rows and colums.
     */
    
    public Matrix(int n) {
        this.m = n;
        this.n = n;
        A = new double[n][n];
        for (int i = 0; i < n; i++) {
            A[i][i] = 1.0;
        }
    }
    
    /** Construct a matrix from a Matrix.
     * @param in    Matrix to create a copy of.
     */
    
    public Matrix(Matrix in) {
        this.m = in.m;
        this.n = in.n;
        A = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                this.A[i][j] = in.A[i][j];
            }
        }
    }
    /** Construct an m-by-n constant matrix.
     * @param m    Number of rows.
     * @param n    Number of colums.
     * @param s    Fill the matrix with this scalar value.
     */
    
    public Matrix(int m, int n, double s) {
        this.m = m;
        this.n = n;
        A = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                A[i][j] = s;
            }
        }
    }
    
    /** Construct a square matrix from a vector.
     * @param X    Put the elements of x on the diagonal.
     */
    
    public Matrix(VectorN X) {
        this.m = X.length;
        this.n = X.length;
        this.A = new double[m][n];
        
        for (int i = 0; i < m; i++) {
            for(int j = 0; j < n; j++) {
                this.A[i][j] = 0.0;
            }
        }
        
        for (int i = 0; i < n; i++) {
            this.A[i][i] = X.x[i];
        }
    }
    
    /** Construct a matrix from a 2D-array.
     * @param B    Two-dimensional array of doubles.
     * @exception  IllegalArgumentException All rows must have the same length
     * @see        #constructWithCopy
     */
    
    public Matrix(double[][] B) {
        m = B.length;
        n = B[0].length;
        A = new double[m][n];
        for (int i = 0; i < m; i++) {
            if (B[i].length != n) {
                throw new IllegalArgumentException("All rows must have the same length : " + n);
            }
            for (int j = 0; j < n; j++) {
                A[i][j] = B[i][j];
            }
        }
    }
    
    /** Construct a matrix from a 2D-array.
     * @param B    Two-dimensional array of doubles.
     * @param m    Number of rows.
     * @param n    Number of columns.
     * @exception  IllegalArgumentException All rows must have the same length
     * @see        #constructWithCopy
     */
    
    public Matrix(double[][] B, int m, int n) {
        this.m = m;
        this.n = n;
        A = new double[m][n];
        for (int i = 0; i < m; i++) {
            if (B[i].length < n) {
                throw new IllegalArgumentException("All rows must have a length >= " + n);
            }
            for (int j = 0; j < n; j++) {
                A[i][j] = B[i][j];
            }
        }
    }
    
    /** Construct a matrix from a one-dimensional packed array
     * @param vals One-dimensional array of doubles, packed by columns (ala Fortran).
     * @param m    Number of rows.
     * @exception  IllegalArgumentException Array length must be a multiple of m.
     */
    
    public Matrix(double vals[], int m) {
        this.m = m;
        n = (m != 0 ? vals.length/m : 0);
        if (m*n != vals.length) {
            throw new IllegalArgumentException("Array length must be a multiple of " + m);
        }
        A = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                A[i][j] = vals[i+j*m];
            }
        }
    }
    
/* ----------------------
   Public Methods
 * ---------------------- */
    
    //////////////////////////////////////////
    //Static constructors for simple matrix.//
    //////////////////////////////////////////
    
    /** Generate matrix with random elements
     * @param m    Number of rows.
     * @param n    Number of colums.
     * @return     An m-by-n matrix with uniformly distributed random elements.
     */
    
    public static Matrix random(int m, int n) {
        Matrix A = new Matrix(m,n);
        double[][] X = A.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                X[i][j] = Math.random();
            }
        }
        return A;
    }
    
    /** Generate identity matrix
     * @param m    Number of rows.
     * @param n    Number of colums.
     * @return     An m-by-n matrix with ones on the diagonal and zeros elsewhere.
     */
    
    public static Matrix identity(int m, int n) {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = (i == j ? 1.0 : 0.0);
            }
        }
        return X;
    }
    
    /** Generate a matrix with a constant pitch beetwen each row
     * @param m    Number of rows.
     * @param n    Number of colums.
     * @param begin    begining value to increment.
     * @param pitch    pitch to add.
     * @return     An m-by-n matrix.
     */
    
    public static Matrix increment(int m, int n,double begin,double pitch) {
        return incrementRows(m,n,begin,pitch);
    }
    
    /** Generate a matrix with a constant pitch beetwen each row
     * @param m    Number of rows.
     * @param n    Number of colums.
     * @param begin    begining value to increment.
     * @param pitch    pitch to add.
     * @return     An m-by-n matrix.
     */
    
    public static Matrix incrementRows(int m, int n,double begin,double pitch) {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = begin+i*pitch;
            }
        }
        return X;
    }
    
    /** Generate a matrix with a constant pitch beetwen each column
     * @param m    Number of rows.
     * @param n    Number of colums.
     * @param begin    begining value to increment.
     * @param pitch    pitch to add.
     * @return     An m-by-n matrix.
     */
    
    public static Matrix incrementColumns(int m, int n,double begin,double pitch) {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = begin+j*pitch;
            }
        }
        return X;
    }
    
    /** Generate a matrix from other matrix.
     * @param Xs    Matrix to merge.
     * @return     An m1+m2+...-by-n matrix.
     */
    
    public static Matrix merge(Matrix[] Xs) {
        return mergeRows(Xs);
    }
    
    /** Generate a matrix from other matrix.
     * @param Xs    Matrix to merge.
     * @return     An m1+m2+...-by-n matrix.
     */
    
    public static Matrix mergeRows(Matrix[] Xs) {
        int n = Xs[0].n;
        int M =0;
        for (int k = 0; k < Xs.length; k++) {
            M = M + Xs[k].m;
        }
        
        Matrix X = new Matrix(M,n);
        double[][] C = X.getArray();
        int ind = 0;
        for (int k = 0; k < Xs.length; k++) {
            for (int i = 0; i < Xs[k].m; i++) {
                for (int j = 0; j < n; j++) {
                    C[i+ind][j] = Xs[k].A[i][j];
                }
            }
            ind = ind + Xs[k].m;
        }
        return X;
    }
    
    /** Generate a matrix from other matrix.
     * @param Xs    Matrix to merge.
     * @return     An m-by-n1+n2+... matrix.
     */
    
    public static Matrix mergeColumns(Matrix[] Xs) {
        int m = Xs[0].m;
        int N =0;
        for (int k = 0; k < Xs.length; k++) {
            N = N + Xs[k].n;
        }
        Matrix X = new Matrix(m,N);
        double[][] C = X.getArray();
        int ind = 0;
        for (int k = 0; k < Xs.length; k++) {
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < Xs[k].n; j++) {
                    C[i][j+ind] = Xs[k].A[i][j];
                }
            }
            ind = ind + Xs[k].n;
        }
        return X;
    }
    
    
    ///////////////////////////////////////////////////////////////////////
    //Basic methods to create, convert into array, clone, or copy matrix.//
    ///////////////////////////////////////////////////////////////////////
    
    /** Make a deep copy of a matrix
     * @return     A matrix.
     */
    
    public Matrix copy() {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j];
            }
        }
        return X;
    }
    
    /** Clone the Matrix object.
     * @return     A matrix Object.
     */
    
    public Object clone() {
        return this.copy();
    }
    
    /** Access the internal two-dimensional array.
     * @return     Pointer to the two-dimensional array of matrix elements.
     */
    
    public double[][] getArray() {
        return A;
    }
    
    /** Copy the internal two-dimensional array.
     * @return     Two-dimensional array copy of matrix elements.
     */
    
    public double[][] getArrayCopy() {
        double[][] C = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j];
            }
        }
        return C;
    }
    
    ////////////////////////////////////////////////////////////
    //Basic and advanced Get methods for matrix and submatrix.//
    ////////////////////////////////////////////////////////////
    
    /** Get a single element.
     * @param i    Row index.
     * @param j    Column index.
     * @return     A(i,j)
     * @exception  ArrayIndexOutOfBoundsException
     */
    
    public double get(int i, int j) {
        return A[i][j];
    }
    
    /** Get a several elements in Column.
     * @param I    Row index.
     * @param J    Column index.
     * @return     A(I(:),J(:))
     * @exception  ArrayIndexOutOfBoundsException
     */
    
    public Matrix get(int[] I, int[] J) {
        checkIndicesLengths(I,J);
        Matrix X = new Matrix(I.length,1);
        double[][] B = X.getArray();
        for (int i = 0; i < I.length; i++) {
            B[i][0] = A[I[i]][J[i]];
        }
        return X;
    }
    
    /** Get a several elements.
     * @param I    Row index.
     * @param J    Column index.
     * @return     A(I(:,:),J(:,:))
     * @exception  ArrayIndexOutOfBoundsException
     */
    
    public Matrix get(int[][] I, int[][] J) {
        checkIndicesDimensions(I,J);
        Matrix X = new Matrix(I.length,I[0].length);
        double[][] B = X.getArray();
        for (int i = 0; i < I.length; i++) {
            for (int j = 0; j < I[i].length; j++) {
                B[i][j] = A[I[i][j]][J[i][j]];
            }
        }
        return X;
    }
    
    /** Get a submatrix.
     * @param i0   Initial row index
     * @param i1   Final row index
     * @param j0   Initial column index
     * @param j1   Final column index
     * @return     A(i0:i1,j0:j1)
     * @exception  ArrayIndexOutOfBoundsException Submatrix indices
     */
    
    public Matrix getMatrix(int i0, int i1, int j0, int j1) {
        Matrix X = new Matrix(i1-i0+1,j1-j0+1);
        double[][] B = X.getArray();
        try {
            for (int i = i0; i <= i1; i++) {
                for (int j = j0; j <= j1; j++) {
                    B[i-i0][j-j0] = A[i][j];
                }
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return X;
    }
    
    /** Copy an internal one-dimensional array from a row.
     * @param l    Row index
     * @return     one-dimensional array copy of matrix elements.
     */
    
    public double[] getRowArrayCopy(int l) {
        double[] C = new double[n];
        for (int i = 0; i < n; i++) {
            C[i] = A[l][i];
        }
        return C;
    }

    /** Copy a row into a VectorN.
     * @param c    Row index
     * @return     VectorN copy of matrix elements.
     */
    
    public VectorN getRowVector(int c) {
        VectorN out = new VectorN(n);
        for (int i = 0; i < n; i++) {
            out.x[i] = A[c][i];
        }
        return out;
    }  
    
    /** Copy an internal one-dimensional array from a row.
     * @param l    Row index
     * @return     one-dimensional array copy of matrix elements.
     */
    
    public Matrix getRow(int l) {
        Matrix X = new Matrix(1,n);
        double[][] C = X.getArray();
        for (int j = 0; j < n; j++) {
            C[0][j] = A[l][j];
        }
        return X;
    }
    
    /** Copy an internal one-dimensional array from many rows.
     * @param l    Rows indexes
     * @return     one-dimensional array copy of matrix elements.
     */
    
    public Matrix getRows(int[] l) {
        Matrix X = new Matrix(l.length,n);
        double[][] C = X.getArray();
        for (int i = 0; i < l.length; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[l[i]][j];
            }
        }
        return X;
    }
    
    /** Copy an internal one-dimensional array from a column.
     * @param c    Column index
     * @return     one-dimensional array copy of matrix elements.
     */
    
    public double[] getColumnArrayCopy(int c) {
        double[] C = new double[m];
        for (int i = 0; i < m; i++) {
            C[i] = A[i][c];
        }
        return C;
    }
    
    /** Copy an internal one-dimensional array from a column.
     * @param c    Column index
     * @return     one-dimensional array copy of matrix elements.
     */
    
    public Matrix getColumn(int c) {
        Matrix X = new Matrix(m,1);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            C[i][0] = A[i][c];
        }
        return X;
    }
    
    /** Copy a column into a VectorN.
     * @param c    Column index
     * @return     VectorN copy of matrix elements.
     */
    
    public VectorN getColumnVector(int c) {
        VectorN out = new VectorN(m);
        for (int i = 0; i < m; i++) {
            out.x[i] = A[i][c];
        }
        return out;
    }    
    
    /** Copy an internal one-dimensional array from a column.
     * @param c    Columns indexes
     * @return     one-dimensional array copy of matrix elements.
     */
    
    public Matrix getColumns(int[] c) {
        Matrix X = new Matrix(m,c.length);
        double[][] C = X.getArray();
        for (int j = 0; j < c.length; j++) {
            for (int i = 0; i < m; i++) {
                C[i][j] = A[i][c[j]];
            }
        }
        return X;
    }
    
    /** Make a one-dimensional row packed copy of the internal array.
     * @return     Matrix elements packed in a one-dimensional array by rows.
     */
    
    public double[] getRowPackedCopy() {
        double[] C = new double[m*n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i*n+j] = A[i][j];
            }
        }
        return C;
    }
    
    /** Make a one-dimensional row packed copy of the internal array.
     * @return     Matrix elements packed in a vector by rows.
     */
    
    public VectorN getRowPackedVector() {
        double [] temp = this.getRowPackedCopy();
        VectorN out = new VectorN(temp);
        return out;
    }
    
    
    /** Make a one-dimensional column packed copy of the internal array.
     * @return     Matrix elements packed in a one-dimensional array by columns.
     */
    
    public double[] getColumnPackedCopy() {
        double[] C = new double[m*n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i+j*m] = A[i][j];
            }
        }
        return C;
    }
    
    /** Make a one-dimensional column packed copy of the internal array.
     * @return     Matrix elements packed in a vector by column.
     */
    
    public VectorN getColumnPackedVector() {
        double [] temp = this.getColumnPackedCopy();
        VectorN out = new VectorN(temp);
        return out;
    }
    
    
    /** Get row dimension.
     * @return     m, the number of rows.
     */
    
    public int getRowDimension() {
        return m;
    }
    
    /** Get column dimension.
     * @return     n, the number of columns.
     */
    
    public int getColumnDimension() {
        return n;
    }
    
    /** Matrix diagonal extraction.
     * @return     An d*1 Matrix of diagonal elements, d = min(m,n).
     */
    
    public Matrix diag() {
        int d = Math.min(m,n);
        Matrix X = new Matrix(d,1);
        double[][] C = X.getArray();
        for (int i = 0; i < d; i++) {
            C[i][0] = A[i][i];
        }
        return X;
    }
    
    /** Matrix diagonal extraction.
     * @param num    diagonal number.
     * @return     Matrix of the n-th diagonal elements.
     */
    
    public Matrix diag(int num) {
        int l = 0;
        if (n<m) {
            if (num>=0) {
                l = n - num;
            } else if (num<(n-m)) {
                l = m + num;
            } else {
                l = n;
            }
        } else {
            if (num<=0) {
                l = m + num;
            } else if (num>(n-m)) {
                l = n - num;
            } else {
                l = m;
            }
        }
        Matrix X = new Matrix(l,1);
        double[][] C = X.getArray();
        for (int i = 0; i < l; i++) {
            C[i][0] = (num>0) ? (A[i][i+num]) : (A[i-num][i]);
        }
        return X;
    }
    
    /** Return the diagonals of a square matrix in a VectorN
     * @return VectorN containing the diagonal elements
     */
    public VectorN diagonal(){
    	VectorN out = new VectorN(this.m);
    	for (int i = 0; i < this.m; i++){
    		out.x[i] = this.A[i][i];
    	}
    	return out;
    }
    
    ////////////////////////////////////////////////////////////
    //Basic and advanced Set methods for matrix and submatrix.//
    ////////////////////////////////////////////////////////////
    
    /** Set a single element.
     * @param i    Row index.
     * @param j    Column index.
     * @param s    A(i,j).
     * @exception  ArrayIndexOutOfBoundsException
     */
    
    
    public void set(int i, int j, double s) {
        A[i][j] = s;
    }
    
    /** Set several elements.
     * @param I    Row index.
     * @param J    Column index.
     * @param s    A(I(:),J(:)).
     * @exception  ArrayIndexOutOfBoundsException
     */
    
    
    public void set(int[] I, int[] J, double s) {
        checkIndicesLengths(I,J);
        for (int i = 0; i < I.length; i++) {
            A[I[i]][J[i]] = s;
        }
    }
    
    /** Set a submatrix.
     * @param i0   Initial row index
     * @param j0   Initial column index
     * @param X   subMatrix to set
     * @exception  ArrayIndexOutOfBoundsException Submatrix indices
     */
    
    public void setMatrix(int i0, int j0, Matrix X) {
        try {
            for (int i = i0; i < i0+X.m; i++) {
                for (int j = j0; j < j0+X.n; j++) {
                    A[i][j] = X.A[i-i0][j-j0];
                }
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
    }
    
    /** Set a submatrix.
     * @param I0   Initial row indexes
     * @param J0   Initial column indexes
     * @param X   subMatrix to set
     * @exception  ArrayIndexOutOfBoundsException Submatrix indices
     */
    
    public void setMatrix(int[] I0, int[] J0, Matrix X) {
        checkIndicesLengths(I0,J0);
        for (int k = 0; k < I0.length; k++) {
            int i0 = I0[k];
            int j0 = J0[k];
            try {
                for (int i = i0; i < i0+X.m; i++) {
                    for (int j = j0; j < j0+X.n; j++) {
                        A[i][j] = X.A[i-i0][j-j0];
                    }
                }
            } catch(ArrayIndexOutOfBoundsException e) {
                throw new ArrayIndexOutOfBoundsException("Submatrix indices");
            }
        }
    }
    
    /** Set a submatrix.
     * @param i0   Initial row index
     * @param i1   Final row index
     * @param j0   Initial column index
     * @param j1   Final column index
     * @param v    Value to set in the submatrix
     * @exception  ArrayIndexOutOfBoundsException Submatrix indices
     */
    
    public void setMatrix(int i0, int i1, int j0, int j1, double v) {
        try {
            for (int i = i0; i <= i1; i++) {
                for (int j = j0; j <= j1; j++) {
                    A[i][j] = v;
                }
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
    }
    
    /** Set a submatrix.
     * @param i0   Initial row index
     * @param i1   Final row index
     * @param j0   Initial column index
     * @param j1   Final column index
     * @param X    A(i0:i1,j0:j1)
     * @exception  ArrayIndexOutOfBoundsException Submatrix indices
     */
    
    public void setMatrix(int i0, int i1, int j0, int j1, Matrix X) {
        X.checkMatrixDimensions(i1-i0+1,j1-j0+1);
        try {
            for (int i = i0; i <= i1; i++) {
                for (int j = j0; j <= j1; j++) {
                    A[i][j] = X.A[i-i0][j-j0];
                }
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
    }
    
    /** Copy an internal one-dimensional array from a row.
     * @param l    Row index
     * @param B    Row-matrix
     */
    
    public void setRow(int l,Matrix B) {
        B.checkMatrixDimensions(1,n);
        for (int j = 0; j < n; j++) {
            A[l][j] = B.A[0][j];
        }
    }
    
    /** Copy an internal one-dimensional array from a row.
     * @param l    Row index
     * @param B    VectorN
     */
    
    public void setRow(int l,VectorN B) {
        B.checkVectorDimensions(n);
        for (int j = 0; j < n; j++) {
            A[l][j] = B.x[j];
        }
    }
    
    /** Copy an internal one-dimensional array from many rows.
     * @param l    Rows indexes
     * @param B    Rows-matrix
     */
    
    public void setRows(int[] l,Matrix B) {
        B.checkMatrixDimensions(l.length,n);
        for (int i = 0; i < l.length; i++) {
            for (int j = 0; j < n; j++) {
                A[l[i]][j] = B.A[i][j];
            }
        }
    }
    
    /** Set a column to an internal one-dimensional Column.
     * @param c    Column index
     * @param B    Column-matrix
     */
    
    public void setColumn(int c,Matrix B) {
        B.checkMatrixDimensions(m,1);
        for (int i = 0; i < m; i++) {
            A[i][c] = B.A[i][0];
        }
    }
    
    /** Set a column to an internal one-dimensional Column.
     * @param c    Column index
     * @param B    VectorN
     */
    
    public void setColumn(int c,VectorN B) {
        B.checkVectorDimensions(m);
        for (int i = 0; i < m; i++) {
            A[i][c] = B.x[i];
        }
    }
    
    /** Copy an internal one-dimensional array from a column.
     * @param c    Columns indexes
     * @param B    Columns-matrix
     */
    
    public void setColumns(int[] c,Matrix B) {
        B.checkMatrixDimensions(m,c.length);
        for (int j = 0;j<c.length;j++) {
            for (int i = 0; i < m; i++) {
                A[i][c[j]] = B.A[i][j];
            }
        }
    }
    
    
    ////////////////////////////////
    //Modify dimensions of matrix.//
    ////////////////////////////////
    
    /** Matrix resize.
     * @param m2    number of rows
     * @param n2    number of columns
     * @return    resized matrix
     */
    
    public Matrix resize(int m2, int n2) {
        Matrix X = new Matrix(m2,n2);
        for (int i = 0; i < m2; i++) {
            for (int j = 0; j < n2; j++) {
                X.A[i][j] = ((i < m) && (j < n)) ? (A[i][j]) : (0);
            }
        }
        return X;
    }
    
    /** Matrix reshape by Row.
     * @param m2    number of rows
     * @param n2    number of columns
     * @return    reshaped matrix
     */
    
    public Matrix reshape(int m2, int n2) {
        return reshapeRows(m2,n2);
    }
    
    /** Matrix reshape by Row.
     * @param m2    number of rows
     * @param n2    number of columns
     * @return    reshaped matrix
     */
    
    public Matrix reshapeRows(int m2, int n2) {
        
        if ((m2*n2) != (m*n)) {
            throw new IllegalArgumentException("Matrix dimensions products must be equals.");
        }
        
        Matrix X = new Matrix(m2,n2);
        double[][] C2 = X.getArray();
        double[] C = getRowPackedCopy();
        for (int i = 0; i < m2; i++) {
            for (int j = 0; j < n2; j++) {
                C2[i][j] = C[(j+1)+(i)*n2-1];
            }
        }
        return X;
    }
    
    /** Matrix reshape by Column.
     * @param m2    number of rows
     * @param n2    number of columns
     * @return    reshaped matrix
     */
    
    public Matrix reshapeColumns(int m2, int n2) {
        
        if ((m2*n2) != (m*n)) {
            throw new IllegalArgumentException("Matrix dimensions products must be equals.");
        }
        
        Matrix X = new Matrix(m2,n2);
        double[][] C2 = X.getArray();
        double[] C = getColumnPackedCopy();
        for (int i = 0; i < m2; i++) {
            for (int j = 0; j < n2; j++) {
                C2[i][j] = C[(i+1)+(j)*m2-1];
            }
        }
        return X;
    }
    
    /** Matrix transpose.
     * @return    A'
     */
    
    public Matrix transpose() {
        Matrix X = new Matrix(n,m);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[j][i] = A[i][j];
            }
        }
        return X;
    }
    
    /** Matrix merge.
     * @param B    matrix to merge
     * @return     An m.B.m-by-n matrix
     */
    
    public Matrix merge(Matrix B) {
        return mergeRows(B);
    }
    
    /** Matrix merge.
     * @param B    matrix to merge
     * @return     An m.B+m-by-n matrix
     */
    
    public Matrix mergeRows(Matrix B) {
        checkColumnDimension(B);
        Matrix X = new Matrix(m+B.m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j];
            }
        }
        for (int i = 0; i < B.m; i++) {
            for (int j = 0; j < n; j++) {
                C[i+m][j] = B.A[i][j];
            }
        }
        return X;
    }
    
    /** Matrix merge.
     * @param B    matrix to merge
     * @return     An m-by-n+B.n matrix
     */
    
    public Matrix mergeColumns(Matrix B) {
        checkRowDimension(B);
        Matrix X = new Matrix(m,n+B.n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j];
            }
        }
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < B.n; j++) {
                C[i][j+n] = B.A[i][j];
            }
        }
        return X;
    }
    
    
    ////////////////////////////////////////
    //Norms and characteristics of Matrix.//
    ////////////////////////////////////////
    
    /** One norm
     * @return    maximum column sum.
     */
    
    public double norm1() {
        double f = 0;
        for (int j = 0; j < n; j++) {
            double s = 0;
            for (int i = 0; i < m; i++) {
                s += Math.abs(A[i][j]);
            }
            f = Math.max(f,s);
        }
        return f;
    }
    
    /** Two norm
     * @return    maximum singular value.
     */
    
 /*  public double norm2() {
        return (new SingularValueDecomposition(this).norm2());
    }
    
    /** Infinity norm
     * @return    maximum row sum.
     */
    
/*    public double normInf() {
        double f = 0;
        for (int i = 0; i < m; i++) {
            double s = 0;
            for (int j = 0; j < n; j++) {
                s += Math.abs(A[i][j]);
            }
            f = Math.max(f,s);
        }
        return f;
    }*/
    
    /** Frobenius norm
     * @return    sqrt of sum of squares of all elements.
     */
    
  /*  public double normF() {
        double f = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                f = Mathfun.hypot(f,A[i][j]);
            }
        }
        return f;
    }
    
    /** Matrix determinant
     * @return     determinant
     */
    
  /*  public double det() {
        return new LUDecomposition(this).det();
    }
    
    /** Matrix rank
     * @return     effective numerical rank, obtained from SVD.
     */
    
  /*  public int rank() {
        return new SingularValueDecomposition(this).rank();
    }
    
    /** Matrix condition (2 norm)
     * @return     ratio of largest to smallest singular value.
     */
    
  /*  public double cond() {
        return new SingularValueDecomposition(this).cond();
    }
    
    /** Matrix trace.
     * @return     sum of the diagonal elements.
     */
    
    public double trace() {
        double t = 0;
        for (int i = 0; i < Math.min(m,n); i++) {
            t += A[i][i];
        }
        return t;
    }
    
    /** Generate a row matrix, each column contents the minimum value of the columns.
     * @return     An 1-by-n matrix.
     */
    
    public Matrix min() {
        return minRows();
    }
    
    /** Generate a row matrix, each column contents the minimum value of the columns.
     * @return     An 1-by-n matrix.
     */
    
    public Matrix minRows() {
        Matrix X = new Matrix(1,n);
        for (int j = 0; j < n; j++) {
            double minval = get(0,j);
            for (int i = 0; i < m; i++)
                minval = Math.min(minval,get(i,j));
            X.set(0,j,minval);
        }
        return X;
    }
    
    /** Generate a column matrix, each line contents the minimum value of the lines.
     * @return     An m-by-1 matrix.
     */
    
    public Matrix minColumns() {
        Matrix X = new Matrix(m,1);
        for (int i = 0; i< m; i++) {
            double minval = get(i,0);
            for (int j = 0; j < n; j++)
                minval = Math.min(minval,get(i,j));
            X.set(i,0,minval);
        }
        return X;
    }
    
    /** Generate a row matrix, each column contents the maximum value of the columns.
     * @return     An 1-by-n matrix.
     */
    
    public Matrix max() {
        return maxRows();
    }
    
    /** Generate a row matrix, each column contents the maximum value of the columns.
     * @return     An 1-by-n matrix.
     */
    
    public Matrix maxRows() {
        Matrix X = new Matrix(1,n);
        double[][] C = X.getArray();
        for (int j = 0; j < n; j++) {
            double maxval = A[0][j];
            for (int i = 0; i < m; i++)
                maxval = Math.max(maxval,A[i][j]);
            C[0][j]=maxval;
        }
        return X;
    }
    
    /** Generate a column matrix, each line contents the maximum value of the lines.
     * @return     An m-by-1 matrix.
     */
    
    public Matrix maxColumns() {
        Matrix X = new Matrix(m,1);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            double maxval = A[i][0];
            for (int j = 0; j < n; j++)
                maxval = Math.max(maxval,A[i][j]);
            C[i][0]=maxval;
        }
        return X;
    }
    
    /** Generate a row matrix, each column contents the sum value of the columns.
     * @return     An 1-by-n matrix.
     */
    
    public Matrix sum() {
        return sumRows();
    }
    
    /** Generate a row matrix, each column contents the sum value of the columns.
     * @return     An 1-by-n matrix.
     */
    
    public Matrix sumRows() {
        Matrix X = new Matrix(1,n);
        double[][] C = X.getArray();
        double s = 0;
        for (int j = 0; j < n; j++) {
            s = 0;
            for (int i = 0; i < m; i++) {
                s = s + A[i][j];
            }
            C[0][j] = s;
        }
        return X;
    }
    
    /** Generate a column matrix, each line contents the sum value of the lines.
     * @return     An m-by-1 matrix.
     */
    
    public Matrix sumColumns() {
        Matrix X = new Matrix(m,1);
        double[][] C = X.getArray();
        double s = 0;
        for (int i = 0; i < m; i++) {
            s = 0;
            for (int j = 0; j < n; j++) {
                s = s + A[i][j];
            }
            C[i][0] = s;
        }
        return X;
    }
    
    /** Generate a row matrix, each column contents the product value of the columns.
     * @return     An 1-by-n matrix.
     */
    
    public Matrix prod() {
        return prodRows();
    }
    
    /** Generate a row matrix, each column contents the product value of the columns.
     * @return     An 1-by-n matrix.
     */
    
    public Matrix prodRows() {
        Matrix X = new Matrix(1,n);
        double[][] C = X.getArray();
        double s = 0;
        for (int j = 0; j < n; j++) {
            s = 1;
            for (int i = 0; i < m; i++) {
                s = s * A[i][j];
            }
            C[0][j] = s;
        }
        return X;
    }
    
    /** Generate a column matrix, each line contents the product value of the lines.
     * @return     An m-by-1 matrix.
     */
    
    public Matrix prodColumns() {
        Matrix X = new Matrix(m,1);
        double[][] C = X.getArray();
        double s = 0;
        for (int i = 0; i < m; i++) {
            s = 1;
            for (int j = 0; j < n; j++) {
                s = s * A[i][j];
            }
            C[i][0] = s;
        }
        return X;
    }
    
    /** Generate a matrix, each column contents the Euclidian distance between the columns.
     * @param B    Matrix
     * @return     An m-by-B.m matrix.
     */
    
    public Matrix dist(Matrix B) {
        return distRows(B);
    }
    
    /** Generate a matrix, each column contents the Euclidian distance between the columns.
     * @param B    Matrix
     * @return     An m-by-B.m matrix.
     */
    
    public Matrix distRows(Matrix B) {
        checkColumnDimension(B);
        Matrix X = new Matrix(m,B.m);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int k = 0; k < B.m; k++) {
                double s = 0;
                for (int j = 0; j < n; j++) {
                    s = s + ((A[i][j] - B.A[k][j]) * (A[i][j] - B.A[k][j]));
                }
                C[i][k] = Math.sqrt(s);
            }
        }
        return X;
    }
    
    /** Generate a matrix, each line contents the Euclidian distance between the lines.
     * @param B    Matrix
     * @return     An B.n-by-n matrix.
     */
    
    public Matrix distColumns(Matrix B) {
        checkRowDimension(B);
        Matrix X = new Matrix(n,B.n);
        double[][] C = X.getArray();
        for (int j = 0; j < n; j++) {
            for (int k = 0; k < B.n; k++) {
                double s = 0;
                for (int i = 0; i < m; i++) {
                    s = s + ((A[i][j] - B.A[i][k]) * (A[i][j] - B.A[i][k]));
                }
                C[k][j] = Math.sqrt(s);
            }
        }
        return X;
    }
    
    ///////////////////////////////////
    //Algebraic Functions for Matrix.//
    ///////////////////////////////////
    
    /**  Unary minus
     * @return    -A
     */
    
    public Matrix uminus() {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = -A[i][j];
            }
        }
        return X;
    }
    
    /** C = A + B
     * @param B    another matrix
     * @return     A + B
     */
    
    public Matrix plus(Matrix B) {
        checkMatrixDimensions(B);
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] + B.A[i][j];
            }
        }
        return X;
    }
    
    /** C = A - B
     * @param B    another matrix
     * @return     A - B
     */
    
    public Matrix minus(Matrix B) {
        checkMatrixDimensions(B);
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] - B.A[i][j];
            }
        }
        return X;
    }
    
    /** Multiply a matrix by a scalar, C = s*A
     * @param s    scalar
     * @return     s*A
     */
    
    public Matrix times(double s) {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = s*A[i][j];
            }
        }
        return X;
    }
    
    /** Linear algebraic matrix multiplication, A * B
     * @param B    another matrix
     * @return     Matrix product, A * B
     * @exception  IllegalArgumentException Matrix inner dimensions must agree.
     */
    
    public Matrix times(Matrix B) {
        if (B.m != n) {
            throw new IllegalArgumentException("Matrix inner dimensions must agree.");
        }
        Matrix X = new Matrix(m,B.n);
        double[][] C = X.getArray();
        double[] Bcolj = new double[n];
        for (int j = 0; j < B.n; j++) {
            for (int k = 0; k < n; k++) {
                Bcolj[k] = B.A[k][j];
            }
            for (int i = 0; i < m; i++) {
                double[] Arowi = A[i];
                double s = 0;
                for (int k = 0; k < n; k++) {
                    s += Arowi[k]*Bcolj[k];
                }
                C[i][j] = s;
            }
        }
        return X;
    }
    
    /** Divide a matrix by a scalar, C = A/s
     * @param s    scalar
     * @return     A/s
     */
    
    public Matrix divide(double s) {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j]/s;
            }
        }
        return X;
    }
    
    /** Linear algebraic matrix division, A / B
     * @param B    another matrix
     * @return     Matrix division, A / B
     * @exception  IllegalArgumentException Matrix inner dimensions must agree.
     * @exception  IllegalArgumentException Matrix inner dimensions must agree.
     */
    
  /*  public Matrix divide(Matrix B) {
        B = B.inverse();
        if (B.m != n) {
            throw new IllegalArgumentException("Matrix inner dimensions must agree.");
        }
        return times(B);
    }
    
    /** Solve A*X = B
     * @param B    right hand side
     * @return     solution if A is square, least squares solution otherwise
     */
    
 /*   public Matrix solve(Matrix B) {
        return (m == n ? (new LUDecomposition(this)).solve(B) :
            (new QRDecomposition(this)).solve(B));
    }
    
    /** Matrix inverse or pseudoinverse
     * @return     inverse(A) if A is square, pseudoinverse otherwise.
     */
    
  /*  public Matrix inverse() {
        return solve(identity(m,m));
    }
    
    /** Matrix multiplied by a column array
     * @param B    array to be post-multiplied
     * @return     A*B
     */
    
    public double[] times(double[] B) {
        checkColumnDimension(B.length);
        double[] out = new double[this.m];
        for (int i = 0; i < this.m; i++) {
            for (int j = 0; j < this.n; j++) {
                out[i] = out[i] + this.A[i][j]*B[j];
            }
        }
        return out;
    }
    
    /** Matrix multiplied by a column vector
     * @param B    vector to be post-multiplied
     * @return     A*B
     */
    
    public VectorN times(VectorN B) {
        checkColumnDimension(B.length);
        double[] temp = new double[this.m];
        for (int i = 0; i < this.m; i++) {
            for (int j = 0; j < this.n; j++) {
                temp[i] = temp[i] + this.A[i][j]*B.get(j);
            }
        }
        VectorN out = new VectorN(temp);
        return out;
    }
    
    
    
    /////////////////////////////////////////////
    //Element-by-elements Functions for Matrix.//
    /////////////////////////////////////////////
    
    /** Add a scalar to each element of a matrix, C = A .+ s
     * @param s    double
     * @return     A .+ s
     */
    
    public Matrix ebePlus(double s) {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] + s;
            }
        }
        return X;
    }
    
    /** Sub a scalar to each element of a matrix, C = A .- B
     * @param s    double
     * @return     A .- s
     */
    
    public Matrix ebeMinus(double s) {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] - s;
            }
        }
        return X;
    }
    
    
    /** Multiply a matrix by a scalar, C = s*A
     * @param s    scalar
     * @return     s*A
     */
    
    public Matrix ebeTimes(double s) {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = s*A[i][j];
            }
        }
        return X;
    }
    
    /** Element-by-element multiplication, C = A.*B
     * @param B    another matrix
     * @return     A.*B
     */
    
    public Matrix ebeTimes(Matrix B) {
        checkMatrixDimensions(B);
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] * B.A[i][j];
            }
        }
        return X;
    }
    
    /** Divide a matrix by a scalar, C = A/s
     * @param s    scalar
     * @return     A/s
     */
    
    public Matrix ebeDivide(double s) {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j]/s;
            }
        }
        return X;
    }
    
    /** Element-by-element right division, C = A./B
     * @param B    another matrix
     * @return     A./B
     */
    
    public Matrix ebeDivide(Matrix B) {
        checkMatrixDimensions(B);
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] / B.A[i][j];
            }
        }
        return X;
    }
    
    /**Element-by-element cosinus
     * @return     cos.(A)
     */
    
    public Matrix ebeCos() {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = Math.cos(A[i][j]);
            }
        }
        return X;
    }
    
    /**Element-by-element sinus
     * @return     sin.(A)
     */
    
    public Matrix ebeSin() {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = Math.sin(A[i][j]);
            }
        }
        return X;
    }
    
    /**Element-by-element exponential
     * @return     exp.(A)
     */
    
    public Matrix ebeExp() {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = Math.exp(A[i][j]);
            }
        }
        return X;
    }
    
    /**Element-by-element power
     * @param p    double
     * @return     A.^p
     */
    
    public Matrix ebePow(double p) {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = Math.pow(A[i][j],p);
            }
        }
        return X;
    }
    
    /**Element-by-element power
     * @param B    another matrix
     * @return     A.^B
     */
    
    public Matrix ebePow(Matrix B) {
        checkMatrixDimensions(B);
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = Math.pow(A[i][j],B.A[i][j]);
            }
        }
        return X;
    }
    
    /**Element-by-element neperian logarithm
     * @return     log.(A)
     */
    
    public Matrix ebeLog() {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = Math.log(A[i][j]);
            }
        }
        return X;
    }
    
    /**Element-by-element inverse
     * @return     A.^-1
     */
    
    public Matrix ebeInv() {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = 1/(A[i][j]);
            }
        }
        return X;
    }
    
    /**Element-by-element inverse
     * @return     A.^-1
     */
    
    public Matrix ebeSqrt() {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = Math.sqrt(A[i][j]);
            }
        }
        return X;
    }
    
    /**Element-by-element inverse
     * @return     A.^-1
     */
    
    public Matrix ebeAbs() {
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = Math.abs(A[i][j]);
            }
        }
        return X;
    }
    
    /**Element-by-element function evaluation
     * @param fun    function to apply
     * @return     f.(A)
     */
    
  /*  public Matrix ebeFun(DoubleFunction fun) {
        fun.checkArgNumber(1);
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                double[] arg = {A[i][j]};
                C[i][j] = fun.eval(arg);
            }
        }
        return X;
    }
    
    /**Element-by-element indicial function evaluation
     * @param fun    function to apply
     * @return     f.(A)
     */
    
/*    public Matrix ebeIndFun(DoubleFunction fun) {
        fun.checkArgNumber(3);
        Matrix X = new Matrix(m,n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                double[] args = {A[i][j],i,j};
                C[i][j] = fun.eval(args);
            }
        }
        return X;
    }
    
    
    ///////////////////////////////////////////////
    //Advanced Decompositions methods for Matrix.//
    ///////////////////////////////////////////////
    
    /** LU Decomposition
     * @return     LUDecomposition
     * @see LUDecomposition
     */
    
  /*  public LUDecomposition lu() {
        return new LUDecomposition(this);
    }
    
    /** QR Decomposition
     * @return     QRDecomposition
     * @see QRDecomposition
     */
    
 /*   public QRDecomposition qr() {
        return new QRDecomposition(this);
    }
    
    /** Cholesky Decomposition
     * @return     CholeskyDecomposition
     * @see CholeskyDecomposition
     */
    
 /*   public CholeskyDecomposition chol() {
        return new CholeskyDecomposition(this);
    }
    
    /** Singular Value Decomposition
     * @return     SingularValueDecomposition
     * @see SingularValueDecomposition
     */
    
 /*   public SingularValueDecomposition svd() {
        return new SingularValueDecomposition(this);
    }
    
    /** Eigenvalue Decomposition
     * @return     EigenvalueDecomposition
     * @see EigenvalueDecomposition
     */
    
  /*  public EigenvalueDecomposition eig() {
        return new EigenvalueDecomposition(this);
    }
    
    
    /////////////////////////////////////////
    //Advanced functions like sort or find.//
    /////////////////////////////////////////
    
    /** Generate a column-permuted matrix, rows are permuted in order to sort the column 'c'
     * @param c    Number of the colum which leads the permuation
     * @return     An Integer Array.
     */
    
/*    public int[] sort(int c) {
        return sortRows(c);
    }
    
    /** Generate a column-permuted matrix, rows are permuted in order to sort the column 'c'
     * @param c    Number of the colum which leads the permuation
     * @return     An m-by-n matrix.
     */
    
 /*   public int[] sortRows(int c) {
        if (c > n) {
            throw new IllegalArgumentException("Matrix Columns dimensions must < " + c);
        }
        int[] I = new int[m];
        Sort order = new Sort(getColumnArrayCopy(c));
        for (int i = 0; i < m; i++) {
            I[i] = order.getOrder(i);
        }
        return I;
    }
    
    /** Generate a row-permuted matrix, columns are permuted in order to sort the row 'l'
     * @param l    Number of the row which leads the permuation
     * @return     An Integer Array.
     */
    
/*    public int[] sortColumns(int l) {
        if (l > m) {
            throw new IllegalArgumentException("Matrix Rows dimensions must < " + l);
        }
        int[] I = new int[n];
        Sort order = new Sort(getRowArrayCopy(l));
        for (int i = 0; i < n; i++) {
            I[i] = order.getOrder(i);
        }
        return I;
    }
    
    /** Generate a column-permuted matrix, rows are permuted in order to sort the column 'c'
     * @param c    Number of the colum which leads the permuation
     * @return     An m-by-n matrix.
     */
    
/*    public Matrix sortedMatrix(int c) {
        return sortedRowsMatrix(c);
    }
    
    /** Generate a column-permuted matrix, rows are permuted in order to sort the column 'c'
     * @param c    Number of the colum which leads the permuation
     * @return     An m-by-n matrix.
     */
    
/*    public Matrix sortedRowsMatrix(int c) {
        if (c > n) {
            throw new IllegalArgumentException("Matrix Columns dimensions must < " + c);
        }
        Matrix X = new Matrix(m,n);
        Sort order = new Sort(getColumnArrayCopy(c));
        for (int i = 0; i < m; i++) {
            X.setRow(i,getRow(order.getOrder(i)));
        }
        return X;
    }
    
    /** Generate a row-permuted matrix, columns are permuted in order to sort the row 'l'
     * @param l    Number of the row which leads the permuation
     * @return     An m-by-n matrix.
     */
    
/*    public Matrix sortedColumnsMatrix(int l) {
        if (l > m) {
            throw new IllegalArgumentException("Matrix Rows dimensions must < " + l);
        }
        Matrix X = new Matrix(m,n);
        Sort order = new Sort(getRowArrayCopy(l));
        for (int i = 0; i < n; i++) {
            X.setColumn(i,getColumn(order.getOrder(i)));
        }
        return X;
    }
    
    /** Find an element
     * @param e    Element (value) to find
     * @return     A list of indices where this element is found.
     */
    
/*    public int[][] find(double e) {
        Find f = new Find(getArrayCopy(),e);
        return f.getIndices();
    }
    
    /** Find elements verifying a boolean test
     * @param test    Test to apply: < > =...
     * @param e    Element (value) to compare
     * @return     A list of indices where this element is found.
     */
    
/*    public int[][] find(String test,double e) {
        Find f = new Find(getArrayCopy(),test,e);
        return f.getIndices();
    }
    
    /** Find an element
     * @param e    Element (value) to find
     * @return     A list of indices where this element is found.
     */
    
/*    public Matrix findMatrix(double e) {
        Find f = new Find(getArrayCopy(),e);
        return new Matrix(f.getDoubleArray());
    }
    
    /** Find elements verifying a boolean test
     * @param test    Test to apply: < > =...
     * @param e    Element (value) to compare
     * @return     A list of indices where this element is found.
     */
    
/*    public Matrix findMatrix(String test,double e) {
        Find f = new Find(getArrayCopy(),test,e);
        return new Matrix(f.getDoubleArray());
    }
    
    /////////////////////////////////////////////////////////
    //Matrix io methods, in panels, frames or command line.//
    /////////////////////////////////////////////////////////
    
    /** Print the Matrix in the Command Line.
     * @param title    title to display in the command line.
     */
    
/*    public void toCommandLine(String title) {
        System.out.println(title + " =");
        System.out.println(MatrixString.printMatrix(this));
    }
    
    /** Save the Matrix in a file.
     * @param fileName    filename to save in.
     */
    
 /*   public void toFile(String fileName) {
        MatrixFile mf = new MatrixFile(fileName,this);
    }
    
    /** Save the Matrix in a file.
     * @param file    file to save in.
     */
    
/*    public void toFile(File file) {
        MatrixFile mf = new MatrixFile(file,this);
    }
    
    /** Load the Matrix from a file.
     * @param fileName    filename of the file to load.
     * @return      A matrix.
     */
    
/*    public static Matrix fromFile(String fileName) {
        MatrixFile mf = new MatrixFile(fileName);
        return mf.getMatrix();
    }
    
    /** Load the Matrix from a file
     * @param file    file to load
     * @return      A matrix
     */
    
/*    public static Matrix fromFile(File file) {
        MatrixFile mf = new MatrixFile(file);
        return mf.getMatrix();
    }
    
    /** Convert the Matrix into a String
     * @return      A String
     */
    
 /*   public String toString() {
        String s = MatrixString.printMatrix(this);
        return s;
    }
    
    /** Load the Matrix from a String
     * @param s    String to load
     * @return      A matrix
     */
    
 /*   public static Matrix fromString(String s) {
        Matrix m = MatrixString.readMatrix(s);
        return m;
    }
    
    /** Print the Matrix in a JTable
     * @return      A Swing JTable in a JPanel
     */
    
 /*   public MatrixTable toPanelTable() {
        return new MatrixTable(this);
    }
    
    /** Print the Matrix data in a Table in a JFrame
     * @param title Title of the JFrame.
     */
    
 /*   public void toFrameTable(String title) {
        FrameView fv = new FrameView(title,toPanelTable());
    }
    
    /** Plot the Matrix in a JPanel
     * @return      A MatrixPlot2D (extends a JPanel)
     */
    
 /*   public MatrixPlot2D toPanelPlot2D() {
        MatrixPlot2D mp2d = new MatrixPlot2D(this);
        return mp2d;
    }
    
    /** Plot the Matrix in a JFrame
     * @param title Title of the JFrame.
     * @return      A MatrixPlot2D (extends a Swing JPanel)
     */
    
/*    public MatrixPlot2D toFramePlot2D(String title) {
        MatrixPlot2D mp2d = toPanelPlot2D();
        FrameView fv = new FrameView(title,mp2d);
        return mp2d;
    }
    
    /** Plot the Matrix in a JPanel
     * @param X    Matrix
     * @return      A MatrixPlot2D (extends a Swing JPanel)
     */
    
 /*   public MatrixPlot2D toPanelPlot2D(Matrix X) {
        MatrixPlot2D mp2d;
        if (X.m==1) {
            mp2d = new MatrixPlot2D(X.times(new Matrix(1,n,1)),this);
            return mp2d;
        } else {
            mp2d = new MatrixPlot2D(X,this);
            return mp2d;
        }
    }
    
    /** Plot the Matrix in a JFrame
     * @param title Title of the JFrame.
     * @param X    Matrix
     * @return      A MatrixPlot2D (extends a Swing JPanel)
     */
    
 /*   public MatrixPlot2D toFramePlot2D(String title,Matrix X) {
        MatrixPlot2D mp2d = toPanelPlot2D(X);
        FrameView fv = new FrameView(title,mp2d);
        return mp2d;
    }
    
    
    /** Plot the Matrix in a JPanel
     * @return      A MatrixPlot3D (extends a Swing JPanel)
     */
    
 /*   public MatrixPlot3D toPanelPlot3D() {
        MatrixPlot3D mp3d = new MatrixPlot3D(this);
        return mp3d;
    }
    
    /** Plot the Matrix in a JFrame
     * @param title Title of the JFrame.
     * @return      A MatrixPlot3D (extends a Swing JPanel)
     */
    
  /*  public MatrixPlot3D toFramePlot3D(String title) {
        MatrixPlot3D mp3d = toPanelPlot3D();
        FrameView fv = new FrameView(title,mp3d);
        return mp3d;
    }
    
    /** Plot the Matrix in a JFrame
     * @param X    Matrix
     * @param Y    Matrix
     * @return      A MatrixPlot3D (extends a Swing JPanel)
     */
    
/*    public MatrixPlot3D toPanelPlot3D(Matrix X,Matrix Y) {
        MatrixPlot3D mp3d;
        if ((X.m==1)&&(Y.m==1)) {
            mp3d = new MatrixPlot3D(X.times(new Matrix(1,n,1)),Y.times(new Matrix(1,n,1)),this.copy());
            return mp3d;
        } else {
            mp3d = new MatrixPlot3D(X,Y,this);
            return mp3d;
        }
    }
    
    /** Plot the Matrix in a Window in a JFrame
     * @param title Title of the JFrame.
     * @param X    Matrix
     * @param Y    Matrix
     * @return      A MatrixPlot3D (extends a Swing JPanel)
     */
    
/*   public MatrixPlot3D toFramePlot3D(String title,Matrix X,Matrix Y) {
        MatrixPlot3D mp3d = toPanelPlot3D(X,Y);
        FrameView fv = new FrameView(title,mp3d);
        return mp3d;
    }
    
    /** Print the Matrix to System.out
     */
    
    public void print() {
        System.out.println(this.m +" X "+this.n+" Matrix:");
        for (int i = 0; i < this.m; i++) {
            System.out.println("");
            for (int j = 0; j < this.n; j++) {
                System.out.print(this.A[i][j]+"\t");
            }
        }
        System.out.println();
        System.out.println("-------------");
    }
    
    /** Print the Matrix to a PrintWriter
     *@param pw   PrintWriter to print to.
     */
    
    public void print(PrintWriter pw) {
        for (int i = 0; i < this.m; i++) {
            
            for (int j = 0; j < this.n; j++) {
                pw.print(this.A[i][j]+"\t");
            }
            pw.println();
        }
    }
    
    /** Print the Matrix to System.out
     * @param title    A title or label for the matrix
     */
    
    public void print(String title) {
        System.out.println(title+": "+this.m +" X "+this.n+" Matrix:");
        for (int i = 0; i < this.m; i++) {
            System.out.println("");
            for (int j = 0; j < this.n; j++) {
                System.out.print(this.A[i][j]+"\t");
            }
        }
        System.out.println();
        System.out.println("-------------");
    }
    
    /** Print the first n rows of Matrix to System.out
     * @param title    A title or label for the matrix
     * @param rows     Number of rows to print
     */
    
    public void printrows(String title, int rows) {
        System.out.println(title+": "+this.m +" X "+this.n+" Matrix:");
        for (int i = 0; i < rows; i++) {
            System.out.println("");
            for (int j = 0; j < this.n; j++) {
                System.out.print(this.A[i][j]+"\t");
            }
        }
        System.out.println();
        System.out.println("-------------");
    }
    
    
    /////////////////////////////////////
    //Matrix Test and checking methods.//
    /////////////////////////////////////
    
    /** Check if size(A) == size(B).
     * @param B    Matrix to test.
     */
    
    public void checkMatrixDimensions(Matrix B) {
        if (B.m != m || B.n != n) {
            throw new IllegalArgumentException("Matrix dimensions must equals " + B.m + " x " + B.n);
        }
    }
    
    /** Check if size(A) == m2*n2.
     * @param m2    Number of rows.
     * @param n2    Number of columns.
     */
    
    public void checkMatrixDimensions(int m2, int n2) {
        if (m2 != m || n2 != n) {
            throw new IllegalArgumentException("Matrix dimensions must equals " + m2 + " x " + n2);
        }
    }
    
    /** Check if indices have the same length.
     * @param i    Indices.
     * @param j    Indices.
     */
    
    public static void checkIndicesLengths(int[] i, int[] j) {
        if (i.length != j.length) {
            throw new IllegalArgumentException("Indices lenghts must equals");
        }
    }
    
    /** Check if indices have the same length.
     * @param i    Indices.
     * @param j    Indices.
     */
    
    public static void checkIndicesDimensions(int[][] i, int[][] j) {
        if ((i.length != j.length) || (i[0].length != j[0].length)) {
            throw new IllegalArgumentException("Indices dimensions must equals");
        }
    }
    
    /** Check if number of Rows(A) == number of Rows(B).
     * @param B    Matrix to test.
     */
    
    public void checkRowDimension(Matrix B) {
        if (B.m != m) {
            throw new IllegalArgumentException("Matrix Rows dimensions must equals " + B.m);
        }
    }
    
    /** Check if number of Rows(A) == row.
     * @param row    number of rows.
     */
    
    public void checkRowDimension(int row) {
        if (row != m) {
            throw new IllegalArgumentException("Matrix Rows dimensions must equals " + row);
        }
    }
    
    /** Check if number of Columns(A) == number of Columns(B).
     * @param B    Matrix to test.
     */
    
    public void checkColumnDimension(Matrix B) {
        if (B.n != n) {
            throw new IllegalArgumentException("Matrix Columns dimensions must equals " + B.n);
        }
    }
    
    /** Check if number of Columns(A) == column.
     * @param column    number of columns.
     */
    
    public void checkColumnDimension(int column) {
        if (column != n) {
            throw new IllegalArgumentException("Matrix Columns dimensions must equals " + column);
        }
    }
    
    /** Invert a matrix.
     * @return the inverse.
     */
    
    public Matrix invert() {
        // Check for a square matrix.
        if (this.m != this.n) {
            System.out.println("Can't take determinant of non-square matrix");
            System.exit(1);
        }
        
        double a[][] = new double[this.m][this.n];
        
        // copy input matrix into the a matrix
        
        for (int i = 0; i < this.m; i++) {
            for (int j = 0; j < this.n; j++) {
                a[i][j] = this.A[i][j];
            }
        }
        
        // set some parameters
        
        double eps = 1.0e-12;
        int kp1 = 0;
        int imax = 0;
        double amax = 0.0;
        double amult, adiv;
        double atmp = 0.0;
        double invtmp = 0.0;
        
        Matrix inverse = new Matrix(this.n);  // declare the output matrix
        int nn = this.n;
        
        // locate maximum magnitude on or below main diagonal
        double determinant = 1.0;
        for (int k = 0; k < nn; k++) {
            if ((k - nn) < 0) {
                imax = k;
                amax = Math.abs(a[k][k]);
                kp1 = k + 1;
                for (int i = kp1; i < nn ; i++) {
                    if (amax - Math.abs(a[i][k]) < 0.0) {
                        imax = i;
                        amax = Math.abs(a[i][k]);
                    }
                }
                // interchange rows imax and k if imax not equal to k
                if ((imax - k) != 0) {
                    for (int j = 0; j < n; j++) {
                        atmp = a[imax][j];
                        a[imax][j] = a[k][j];
                        a[k][j] = atmp;
                        invtmp = inverse.A[imax][j];
                        inverse.A[imax][j] = inverse.A[k][j];
                        inverse.A[k][j] = invtmp;
                    }
                    determinant = -determinant;
                }
            }
            // test for singular matrix
            if ((Math.abs(a[k][k]) - eps) > 0.0) {
                determinant = a[k][k] * determinant;
                // divide pivot row by its main diagonal element
                adiv = a[k][k];
                for (int j = 0; j < nn ; j++) {
                    a[k][j] = a[k][j] / adiv;
                    inverse.A[k][j] = inverse.A[k][j] / adiv;
                }
                // replace each row by linear combination with pivot row
                for (int i = 0; i < n; i++) {
                    amult = a[i][k];
                    if ((i - k) != 0) {
                        for (int j = 0; j < n; j++) {
                            a[i][j] = a[i][j] - amult * a[k][j];
                            inverse.A[i][j] = inverse.A[i][j] - amult * inverse.A[k][j];
                        }
                    }
                }
            }
            else {
                // Handle singular matrix
                System.out.println(" Singular matrix for k = "+k);
                //             System.exit(1);
                // for the moment set all the elements of the invese matrix be
                // zero,  in fact the inverse matrix does not exist
                for (int i = 0; i<nn; i++) {
                    for (int j = 0; j<nn; j++) {
                        inverse.A[i][j] = 0.0;
                    }
                }
                determinant = 0.0;
//                this.det = 0.0;
                return inverse;
            }
        }
//        this.det = determinant;
        return inverse;
    }
}
