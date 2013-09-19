package de.uniluebeck.itm.tr.common.dto;

import javax.annotation.Nullable;

public class OutdoorCoordinatesDto {

	@Nullable
	private Float longitude;

	@Nullable
	private Float latitude;

	@Nullable
	private Float x;

	@Nullable
	private Float y;

	@Nullable
	private Float z;

	@Nullable
	private Float rho;

	@Nullable
	private Float phi;

	@Nullable
	private Float theta;

	@Nullable
	public Float getLatitude() {
		return latitude;
	}

	public void setLatitude(@Nullable final Float latitude) {
		this.latitude = latitude;
	}

	@Nullable
	public Float getLongitude() {
		return longitude;
	}

	public void setLongitude(@Nullable final Float longitude) {
		this.longitude = longitude;
	}

	@Nullable
	public Float getPhi() {
		return phi;
	}

	public void setPhi(@Nullable final Float phi) {
		this.phi = phi;
	}

	@Nullable
	public Float getRho() {
		return rho;
	}

	public void setRho(@Nullable final Float rho) {
		this.rho = rho;
	}

	@Nullable
	public Float getTheta() {
		return theta;
	}

	public void setTheta(@Nullable final Float theta) {
		this.theta = theta;
	}

	@Nullable
	public Float getX() {
		return x;
	}

	public void setX(@Nullable final Float x) {
		this.x = x;
	}

	@Nullable
	public Float getY() {
		return y;
	}

	public void setY(@Nullable final Float y) {
		this.y = y;
	}

	@Nullable
	public Float getZ() {
		return z;
	}

	public void setZ(@Nullable final Float z) {
		this.z = z;
	}
}
