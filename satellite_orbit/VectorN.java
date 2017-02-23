package satellite_orbit;

import java.io.PrintWriter;
import java.io.Serializable;

import javax.vecmath.Vector3d;

/**
 * <P>
 * The VectorN Class provides the fundamental operations of numerical vector math,
 * basic manipulations, and visualization tools.
 * All the operations in this version of the VectorN Class involve only real matrices.
 *
 * @author 
 * @version 1.0
 */

public class VectorN implements Cloneable, Serializable {

	/* ------------------------
	   Class variables
	 * ------------------------ */

	/**
	 * 
	 */
	private static final long serialVersionUID = -5400346413116110237L;

	/** Array for internal storage of elements.
	 * @serial internal array storage.
	 */
	public double[] x;

	/** Row and column dimensions.
	 * @serial vector dimension.
	 */
	public int length;

	/* ------------------------
	   Constructors
	 * ------------------------ */

	/** Construct an n-length vector of zeros.
	 * @param n    Number of rows.
	 */

	public VectorN(int n) {
		this.length = n;
		this.x = new double[n];
		for (int i = 0; i < n; i++) {
			this.x[i] = 0.0;
		}
	}

	/** Construct an n-length constant vector.
	 * @param n    Number of rows.
	 * @param s    Fill the vector with this scalar value.
	 */

	public VectorN(int n, double s) {
		this.length = n;
		this.x = new double[n];
		for (int i = 0; i < n; i++) {
			x[i] = s;
		}
	}

	/** Construct a vector from an array.
	 * @param B    One-dimensional array of doubles.
	 */

	public VectorN(double[] B) {
		this.length = B.length;
		this.x = new double[this.length];
		for (int i = 0; i < this.length; i++) {
			this.x[i] = B[i];
		}
	}

	/** Construct a vector from the first n elements of an array.
	 * @param B    One-dimensional array of doubles.
	 * @param n    Number of elements
	 */

	public VectorN(double[] B, int n) {
		if (B.length < n) {
			throw new IllegalArgumentException(
				"Vector dimensions must be greater than or equal to " + n);
		}
		this.length = n;
		this.x = new double[n];
		for (int i = 0; i < n; i++) {
			this.x[i] = B[i];
		}
	}
	/** Construct a vector from an array.
	 * @param B    One-dimensional array of Doubles.
	 */

	public VectorN(Double[] B) {
		int n = B.length;
		System.out.println("n = "+n);
		this.length = n;
		this.x = new double[n];
		for (int i = 0; i < n; i++) {
			this.x[i] = B[i].doubleValue();
		}
	}

	/** Construct a vector from the first n elements of an array.
	 * @param B    One-dimensional array of Doubles.
	 * @param n    Number of elements
	 */

	public VectorN(Double[] B, int n) {
		if (B.length < n) {
			throw new IllegalArgumentException(
				"Vector dimensions must be greater than or equal to " + n);
		}
		this.length = n;
		this.x = new double[n];
		for (int i = 0; i < n; i++) {
			this.x[i] = B[i].doubleValue();
		}
	}	
	
	/** Construct a vector from another vector.
	 * @param y    VectorN.
	 */

	public VectorN(VectorN y) {
		this.length = y.length;
		this.x = new double[this.length];
		for (int i = 0; i < this.length; i++) {
			this.x[i] = y.x[i];
		}
	}

	/** Construct a vector by appending one vector to another vector.
	 * @param y    VectorN.
	 * @param z    VectorN.
	 */
	public VectorN(VectorN y, VectorN z) {
		this.length = y.length + z.length;
		this.x = new double[this.length];
		for (int i = 0; i < y.length; i++) {
			this.x[i] = y.x[i];
		}
		for (int i = 0; i < z.length; i++) {
			int j = i + y.length;
			this.x[j] = z.x[i];
		}

	}

	/** Construct a VectorN from 3 doubles.
	 * @param x    first element.
	 * @param y    second element.
	 * @param z    third element.
	 */

	public VectorN(double x, double y, double z) {
		this.length = 3;
		this.x = new double[3];
		this.x[0] = x;
		this.x[1] = y;
		this.x[2] = z;
	}
	
	/** Construct a VectorN from a Vector3d.
	 * @param x    first element.
	 * @param y    second element.
	 * @param z    third element.
	 */

	public VectorN(Vector3d in) {
		this.length = 3;
		this.x = new double[3];
		this.x[0] = in.x;
		this.x[1] = in.y;
		this.x[2] = in.z;
	}	
	/* ----------------------
	   Public Methods
	 * ---------------------- */
	 
	 //////////////////////////////////
	 // Append methods
	 /////////////////////////////////
	 
	/** Construct a vector by appending a vector to this vector.
	 * @param in    VectorN to append to this one.
	 * @return    new VectorN.
	 */
	 public VectorN append(VectorN in) {
		int length = this.length + in.length;
		VectorN out = new VectorN(length);
		for (int i = 0; i < this.length; i++) {
			out.x[i] = this.x[i];
		}
		for (int i = 0; i < in.length; i++) {
			int j = i + this.length;
			out.x[j] = in.x[i];
		}
		return out;
	 }

	/** Construct a vector by appending a double to this vector.
	 * @param in    double to append.
	 * @return    new VectorN.
	 */
	 public VectorN append(double in) {
		int length = this.length + 1;
		VectorN out = new VectorN(length);
		for (int i = 0; i < this.length; i++) {
			out.x[i] = this.x[i];
		}
		out.x[this.length] = in;
		return out;
	 }	 	

	///////////////////////////////////////////////////////////////////////
	//Basic methods to create, convert into array, clone, or copy VectorN.//
	///////////////////////////////////////////////////////////////////////


	/** Make a deep copy of a vector
	 * @return     X vector.
	 */

	public VectorN copy() {
		VectorN X = new VectorN(this);
		return X;
	}

	/** Clone the VectorN object.
	 * @return     A VectorN Object.
	 */

	public Object clone() {
		return this.copy();
	}

	/** Access the internal one-dimensional array.
	 * @return     Pointer to the one-dimensional array of matrix elements.
	 */

	public double[] getArray() {
		return x;
	}

	/** Copy the internal one-dimensional array.
	 * @return     One-dimensional array copy of vector elements.
	 */

	public double[] getArrayCopy() {
		double[] C = new double[this.length];
		for (int i = 0; i < this.length; i++) {
			C[i] = x[i];
		}
		return C;
	}

	////////////////////////////////////////////////////////////
	//Basic and advanced Get methods for VectorN.             //
	////////////////////////////////////////////////////////////

	/** Get a single element.
	 * @param i    Row index.
	 * @return     x(i)
	 * @exception  ArrayIndexOutOfBoundsException
	 */

	public double get(int i) {
		return x[i];
	}

	/** Get the length of the vector.
	 * @return     length of the vector.
	 */

	public int getLength() {
		return this.length;
	}

	/** Get a subvector from consecutive elements.
	 * @param i    Index of first element.
	 * @param n    Number of elements to be retrieved.
	 * @return     VectorN containing n elements.
	 * @exception  ArrayIndexOutOfBoundsException
	 */

	public VectorN get(int i, int n) {
		VectorN out = new VectorN(n);
		for (int j = 0; j < n; j++) {
			out.x[j] = this.x[i + j];
		}
		return out;
	}
	////////////////////////////////////////////////////////////
	//Basic and advanced Set methods for VectorN.//
	////////////////////////////////////////////////////////////

	/** Set a single element.
	 * @param i    Row index.
	 * @param s    value for x(i).
	 * @exception  ArrayIndexOutOfBoundsException
	 */

	public void set(int i, double s) {
		this.x[i] = s;
	}

	/** Set a set of consecutive elements.
	 * @param i     Index of first element to be set.
	 * @param in    VectorN containing values.
	 * @exception   ArrayIndexOutOfBoundsException
	 */

	public void set(int i, VectorN in) {
		int end = i + in.length - 1;
		if (this.length < end) {
			throw new IllegalArgumentException(
				"Vector dimensions must be equal or greater than "+end);
		}
			
		for (int j = 0; j < in.length; j++) {
			this.x[i + j] = in.x[j];
		}
	}

	/** Set all elements to a single value.
	 * @param s    value for x(i).
	 */

	public void set(double s) {
		for (int i = 0; i < this.length; i++) {
			this.x[i] = s;
		}
	}
	////////////////////////////////////////
	//Norms and characteristics of VectorN.//
	////////////////////////////////////////

	/** Vector magnitude
	 * @return    sqrt of sum of squares of all elements.
	 */

	public double mag() {
		double out = 0.0;
		for (int i = 0; i < this.length; i++) {
			out = out + this.x[i] * this.x[i];
		}
		out = Math.sqrt(out);
		return out;
	}

	/** Distance between two vectors
	 * @param b   the second vector.
	 * @return    distance between two vectors.
	 */

	public double dist(VectorN b) {
		checkVectorDimensions(b);
		VectorN temp = this.minus(b);
		double out = temp.mag();
		return out;
	}

	///////////////////////////////////
	//Algebraic Functions for Matrix.//
	///////////////////////////////////

	/** C = A + B
	 * @param B    another vector
	 * @return     this vector + B
	 */

	public VectorN plus(VectorN B) {
		checkVectorDimensions(B);
		VectorN out = new VectorN(this.length);

		for (int i = 0; i < this.length; i++) {
			out.x[i] = this.x[i] + B.x[i];
		}
		return out;
	}

	/** C = A - B
	 * @param B    another vector
	 * @return     this vector - B
	 */

	public VectorN minus(VectorN B) {
		checkVectorDimensions(B);
		VectorN out = new VectorN(this.length);

		for (int i = 0; i < this.length; i++) {
			out.x[i] = this.x[i] - B.x[i];
		}
		return out;
	}

	/** Multiply a vector by a scalar, C = s*A
	 * @param s    scalar
	 * @return     s*A
	 */

	public VectorN times(double s) {
		VectorN out = new VectorN(this.length);

		for (int i = 0; i < this.length; i++) {
			out.x[i] = this.x[i] * s;
		}
		return out;
	}

	/** Multiply a row vector by a matrix
	 * @param p    Matrix
	 * @return     product of row vector and a matrix
	 */
	public VectorN times(Matrix p) {
		
		p.checkMatrixDimensions(this.length, this.length);

		VectorN out = new VectorN(this.length);
		for (int i = 0; i < this.length; i++) {
			for (int j = 0; j < this.length; j++) {
				out.x[i] = out.x[i] + this.x[j] * p.A[j][i];
			}
		}

		return out;
	}

	/** Dot Product, A * B
	 * @param B    another vector
	 * @return     Dot product, A * B
	 * @exception  IllegalArgumentException Matrix inner dimensions must agree.
	 */

	public double dotProduct(VectorN B)
	// computes the dot product of a * b
	{
		checkVectorDimensions(B);

		double out = 0.0;
		for (int i = 0; i < this.length; i++) {
			out = out + this.x[i] * B.x[i];
		}
		return out;
	}

	/** Outer Product, A * B
	 * @param B    another vector
	 * @return     Matrix containing outer product, A * B
	 * @exception  IllegalArgumentException Matrix inner dimensions must agree.
	 */

	public Matrix outerProduct(VectorN B) {
		checkVectorDimensions(B);
		Matrix out = new Matrix(this.length, this.length);

		for (int i = 0; i < this.length; i++) {
			for (int j = 0; j < this.length; j++) {
				out.A[i][j] = this.x[i] * B.x[j];
			}
		}
		return out;
	}

	/** Divide a vector by a scalar, C = A/s
	 * @param s    scalar
	 * @return     A/s
	 */

	public VectorN divide(double s) {
		VectorN out = new VectorN(this.length);

		for (int i = 0; i < this.length; i++) {
			out.x[i] = this.x[i] / s;
		}
		return out;
	}

	/////////////////////////////////////////////
	//Element-by-elements Functions for vector.//
	/////////////////////////////////////////////

	/** Add a scalar to each element of a vector, C = A .+ s
	 * @param s    double
	 * @return     A .+ s
	 */

	public VectorN ebePlus(double s) {
		VectorN X = new VectorN(this.length);
		for (int i = 0; i < this.length; i++) {
			X.x[i] = this.x[i] + s;
		}
		return X;
	}

	/** Sub a scalar to each element of a vector, C = A .- B
	 * @param s    double
	 * @return     A .- s
	 */

	public VectorN ebeMinus(double s) {
		VectorN out = new VectorN(this.length);
		for (int i = 0; i < this.length; i++) {
			out.x[i] = this.x[i] - s;
		}
		return out;
	}

	/** Element-by-element multiplication, C = A.*B
	 * @param B    another matrix
	 * @return     A.*B
	 */

	public VectorN ebeTimes(VectorN B) {
		checkVectorDimensions(B);
		VectorN out = new VectorN(this.length);
		for (int i = 0; i < this.length; i++) {
			out.x[i] = this.x[i] * B.x[i];
		}
		return out;
	}

	/** Element-by-element division, C = A.*B
	 * @param B    another matrix
	 * @return     A.*B
	 */

	public VectorN ebeDivide(VectorN B) {
		checkVectorDimensions(B);
		VectorN out = new VectorN(this.length);
		for (int i = 0; i < this.length; i++) {
			out.x[i] = this.x[i] / B.x[i];
		}
		return out;
	}

	/**Element-by-element exponential
	 * @return     exp.(A)
	 */

	public VectorN ebeExp() {
		VectorN out = new VectorN(this.length);
		for (int i = 0; i < this.length; i++) {
			out.x[i] = Math.exp(this.x[i]);
		}
		return out;
	}

	/**Element-by-element power
	 * @param p    double
	 * @return     A.^p
	 */

	public VectorN ebePow(double p) {
		VectorN out = new VectorN(this.length);
		for (int i = 0; i < this.length; i++) {
			out.x[i] = Math.pow(this.x[i], p);
		}
		return out;
	}

	/**Element-by-element power
	 * @param B    another matrix
	 * @return     A.^B
	 */

	public VectorN ebePow(VectorN B) {
		checkVectorDimensions(B);
		VectorN out = new VectorN(this.length);
		for (int i = 0; i < this.length; i++) {
			out.x[i] = Math.pow(this.x[i], B.x[i]);
		}
		return out;
	}

	/**Element-by-element neperian logarithm
	 * @return     log.(A)
	 */

	public VectorN ebeLog() {
		VectorN out = new VectorN(this.length);
		for (int i = 0; i < this.length; i++) {
			out.x[i] = Math.log(this.x[i]);
		}
		return out;
	}

	/**Element-by-element inverse
	 * @return     A.^-1
	 */

	public VectorN ebeInv() {
		VectorN out = new VectorN(this.length);
		for (int i = 0; i < this.length; i++) {
			out.x[i] = 1.0 / this.x[i];
		}
		return out;
	}

	/**Element-by-element square root
	 * @return     A.^1/2
	 */

	public VectorN ebeSqrt() {
		VectorN out = new VectorN(this.length);
		for (int i = 0; i < this.length; i++) {
			out.x[i] = Math.sqrt(this.x[i]);
		}
		return out;
	}

	/**Element-by-element absolute value
	 * @return     A.^-1
	 */

	public VectorN ebeAbs() {
		VectorN out = new VectorN(this.length);
		for (int i = 0; i < this.length; i++) {
			out.x[i] = Math.abs(this.x[i]);
		}
		return out;
	}

	///////////////////////////////////////////////
	//Unit vector methods.                       //
	///////////////////////////////////////////////

	/**Creates a unit vector pointing in direction of this vector
	 * @return     a unit vector pointing in direction of this vector
	 */

	public VectorN unitVector() {
		double mag = this.mag();
		VectorN out = this.divide(mag);
		return out;
	}

	/**Converts this vector into a unit vector
	 */

	public void unitize() {
		double mag = this.mag();
		for (int i = 0; i < this.length; i++) {
			this.x[i] = this.x[i] / mag;
		}
	}

	/////////////////////////////////////////
	//Advanced functions like sort or find.//
	/////////////////////////////////////////

	/** Return the average of the vector elements
	 * @return the average of the vector elements
	 */
	public double average(){
		double sum = 0.0;
		for (int i = 0; i < this.length; i++) {
			sum = sum + this.x[i];
		}
		double n = (double)this.length;
		double out = sum/n;
		return out;
	}
	
	/** Return the sample variance of the vector elements
	 * @return the sample variance of the vector elements
	 */
	public double variance(){
		double mean = this.average();
		double sum = 0.0;
		for (int i = 0; i < this.length; i++) {
			double var = this.x[i] - mean;
			double var2 = var * var;
			sum = sum + var2;
		}
		double n = (double)this.length;
		double out = sum/(n - 1.0);
		return out;
	}

	/** Return the sample standard deviation of the vector elements
	 * @return the sample standard deviation of the vector elements
	 */
	public double sigma(){
		double var = this.variance();
		double out = Math.sqrt(var);
		return out;
	}
    
    /**
     * Compares two vectors to see if they have the same
     * values.
     * @param o another object (hopefully a vector)
     * @return true if o is a vector with the same dimensions
     * and same values as this.  false otherwise.
     */
    public boolean equals(Object o) {
      boolean equals = false;
      if (o instanceof VectorN) {
        VectorN v = (VectorN)o;
        if (this.length == v.length) {
          equals = true;
          for(int ctr=0; equals && (ctr<this.length); ++ctr) {
            equals = (this.get(ctr) == v.get(ctr));
          }
        }
      }
      return equals;
    }

	//////////////////////////////
	//Vector3 only methods.     //
	//////////////////////////////

	/** Make a Vector3d copy of a vector
	 * @return  a Vector3d copy of the VectorN.
	 */

	public Vector3d toVector3d() {
		this.checkVectorDimensions(3);		
		Vector3d out = new Vector3d();
		out.x = this.x[0];
		out.y = this.x[1];
		out.z = this.x[2];
		return out;
	}
	/** Construct a Cartesian Vector3 from a Spherical Vector3 containing [r, theta, phi], angles in radians.
	 * @return out     Cartesian Vector3 containing: [x, y, z]
	 */

	public VectorN spherical2Cartesian() {
		this.checkVectorDimensions(3);
		double rmag = this.get(0);
		double theta = this.get(1);
		double phi = this.get(2);
		VectorN out = new VectorN(3);
		double sin_theta = Math.sin(theta);
		double cos_theta = Math.cos(theta);
		double sin_phi = Math.sin(phi);
		double cos_phi = Math.cos(phi);
		out.x[0] = rmag * cos_theta * sin_phi;
		out.x[1] = rmag * sin_theta * sin_phi;
		out.x[2] = rmag * cos_phi;
		return out;
	}

	/** Construct a Cartesian Vector3 from a Cylindrical Vector3 containing: [r, theta, z], theta in radians.
	 * @return out     Cartesian Vector3 containing: [x, y, z]
	 */

	public VectorN cylindrical2Cartesian() {
		this.checkVectorDimensions(3);
		double rmag = this.get(0);
		double theta = this.get(1);
		double z = this.get(2);
		VectorN out = new VectorN(3);
		double sin_theta = Math.sin(theta);
		double cos_theta = Math.cos(theta);
		out.x[0] = rmag * cos_theta;
		out.x[1] = rmag * sin_theta;
		out.x[2] = z;
		return out;
	}

	/** Construct a Spherical Vector3 from a Cartesian Vector3 containing: [x, y, z].
	 * @return out     Spherical Vector3 containing: [r, theta, phi], angles in radians.
	 */

	public VectorN cartesian2Spherical() {
		this.checkVectorDimensions(3);
		double x = this.get(0);
		double y = this.get(1);
		double z = this.get(2);
		double rmag = this.mag();
		double phi = Math.acos(z / rmag);
		double theta = Math.acos(x / (rmag * Math.sin(phi)));
		VectorN out = new VectorN(3);
		out.x[0] = rmag;
		out.x[1] = theta;
		out.x[2] = phi;
		return out;
	}

	/** Construct a Cylindrical Vector3 from a Cartesian Vector3 containing: [x, y, z].
	 * @return out     Cylindrical Vector3 containing: [r, theta, z], theta in radians
	 */

	public VectorN cartesian2Cylindrical() {
		this.checkVectorDimensions(3);
		double x = this.get(0);
		double y = this.get(1);
		double z = this.get(2);
		double rmag = Math.sqrt(x * x + y * y);
		double theta = Math.atan2(y, x);
		VectorN out = new VectorN(3);
		out.x[0] = rmag;
		out.x[1] = theta;
		out.x[2] = z;
		return out;
	}

	/** Cross Product, A X B
	 * @param B    another vector
	 * @return     Cross product, A X B
	 * @exception  IllegalArgumentException Matrix inner dimensions must agree.
	 */

	public VectorN crossProduct(VectorN B) {
		checkVectorDimensions(3);
		checkVectorDimensions(B);
		VectorN out = new VectorN(3);
		out.x[0] = this.x[1] * B.x[2] - this.x[2] * B.x[1];
		out.x[1] = this.x[2] * B.x[0] - this.x[0] * B.x[2];
		out.x[2] = this.x[0] * B.x[1] - this.x[1] * B.x[0];
		return out;
	}

	/** Find the angle between two vectors
	 * @param B    another vector
	 * @return     angle between them in radians
	 * @exception  IllegalArgumentException Matrix inner dimensions must agree.
	 */

	public double angle(VectorN B)
	// finds the angle in radians between two vectors
	{
		checkVectorDimensions(3);
		checkVectorDimensions(B);
		double out = 0.0;
		double mag_a = this.mag();
		double mag_b = B.mag();
		double adotb = this.dotProduct(B);
		out = Math.acos(adotb / (mag_a * mag_b));
		return out;
	}

	/** Find the projection of this vector onto another vector
	 * @param A    another vector
	 * @return     vector projection
	 * @exception  IllegalArgumentException VectorN inner dimensions must agree.
	 */

	public VectorN projection(VectorN A)
	{
		checkVectorDimensions(3);
		checkVectorDimensions(A);
		double mag_a = A.mag();
		double adotb = this.dotProduct(A);
		double mag = adotb/(mag_a * mag_a);
		VectorN out = A.times(mag);
		return out;
	}
	
	/** Find the magnitude of the projection of this vector onto another vector
	 * @param A    another vector
	 * @return     magnitude of the vector projection
	 * @exception  IllegalArgumentException VectorN inner dimensions must agree.
	 */

	public double projectionMag(VectorN A)
	{
		checkVectorDimensions(3);
		checkVectorDimensions(A);
		VectorN temp = this.projection(A);
		double out = temp.mag();
		return out;
	}

	/**
	 * Return the skew symmetric cross-product matrix form of
	 * this vector
	 * @return the skew symmetric cross-product matrix
	 */	
	public Matrix cross(){
		checkVectorDimensions(3);
		double wx = this.x[0];
		double wy = this.x[1];
		double wz = this.x[2];
		Matrix out = new Matrix(3,3);
		out.set(0, 1, -1.0*wz);
		out.set(0, 2, wy);
		out.set(1, 0, wz);
		out.set(1, 2, -1.0*wx);
		out.set(2, 0, -1.0*wy);
		out.set(2, 1, wx);
		return out;
	}
			
	/////////////////////////////////////////////////////////
	//Vector io methods, in panels, frames or command line.//
	//  note: need to add in to/from file methods, etc     //
	/////////////////////////////////////////////////////////

	/** Print the VectorN to System.out.
	 * @param title    title to display in the command line.
	 */

	public void print(String title) {
		System.out.println(" Vector " + title);
		for (int i = 0; i < this.length; i++) {
			System.out.println("  " + title + "[" + i + "] = " + this.x[i]);
		}
		System.out.println("-------------------");
	}

	/** Print the VectorN to System.out.
	 */

	public void print()
	// provides a way to print the elements of the vector
	{
		System.out.println(" Vector ");
		for (int i = 0; i < this.length; i++) {
			System.out.println("  element[" + i + "] = " + this.x[i]);
		}
		System.out.println("-------------------");
	}

	/** Print the VectorN to a PrintWriter
	 *@param pw   PrintWriter to print to.
	 */

	public void print(PrintWriter pw) {
		for (int j = 0; j < this.length; j++) {
			pw.print(this.x[j] + "\t");
		}
		pw.println();
	}

	/** Print selected elements of VectorN to a PrintWriter
	 *@param pw   PrintWriter to print to.
	 *@param i    Integer array containing indices of elements to print.
	 */

	public void print(PrintWriter pw, int[] i) {
		if (i.length > this.length) {
			System.out.println("VectorN.print: too many elements to print");
			return;
		}
		for (int j = 0; j < i.length; j++) {
			pw.print(this.x[i[j]] + "\t");
		}
		pw.println();
	}
	
	public String toString(){
		String out = new String();
		for (int j = 0; j < this.length; j++){
			String number = Double.toString(this.x[j]);
			out = out + number + "\t";
		}
		return out;
	}
	/** Conversion to double array for use in Matlab
	 *  @param none
	 *  @return array of doubles
	 *  @author DMS
	 */  
	public double[] toDouble(){
		double[] out = new double[this.length];
		for (int j = 0; j < this.length; j++){
			out[j] = this.x[j];
		}
		return out;
	}

	/////////////////////////////////////
	//Vector Test and checking methods.//
	/////////////////////////////////////

	/** Check if size(A) == size(B).
	 * @param B    Vector to test.
	 */

	public void checkVectorDimensions(VectorN B) {
		if (B.length != this.length) {
			throw new IllegalArgumentException(
				"Vector dimensions must equal " + this.length);
		}
	}

	/** Check if size(A) == n.
	 * @param B    Vector to test.
	 */

	public void checkVectorDimensions(int n) {
		if (this.length != n) {
			throw new IllegalArgumentException(
				"Vector dimensions must equal " + n);
		}
	}

}
