package de.itm.uniluebeck.tr.wiseml.merger.structures;


public class Coordinate {
	
	private Double x;
    private Double y;
    private Double z;
    private Double phi;
    private Double theta;
    
	public Double getX() {
		return x;
	}
	public void setX(Double x) {
		this.x = x;
	}
	public Double getY() {
		return y;
	}
	public void setY(Double y) {
		this.y = y;
	}
	public Double getZ() {
		return z;
	}
	public void setZ(Double z) {
		this.z = z;
	}
	public Double getPhi() {
		return phi;
	}
	public void setPhi(Double phi) {
		this.phi = phi;
	}
	public Double getTheta() {
		return theta;
	}
	public void setTheta(Double theta) {
		this.theta = theta;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Coordinate)) {
			return false;
		}
		
		Coordinate other = (Coordinate)obj;
		return equals(this.x, other.x)
			&& equals(this.y, other.y)
			&& equals(this.z, other.z)
			&& equals(this.phi, other.phi)
			&& equals(this.theta, other.theta);
	}
	
	public boolean isComparableTo(Coordinate other) {
		return isComparableTo(this.x, other.x)
			&& isComparableTo(this.y, other.y)
			&& isComparableTo(this.z, other.z)
			&& isComparableTo(this.phi, other.phi)
			&& isComparableTo(this.theta, other.theta);
	}
	
	private static boolean isComparableTo(Double a, Double b) {
		return (a == null && b == null) || (a != null && b != null);
	}
	
	private static boolean equals(Object a, Object b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}

}
