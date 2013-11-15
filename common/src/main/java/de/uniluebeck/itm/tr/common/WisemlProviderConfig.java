package de.uniluebeck.itm.tr.common;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;

import javax.annotation.Nullable;

public class WisemlProviderConfig {

	@PropConf(usage = "A human readable description of the testbed")
	public static final String DESCRIPTION = "portal.wiseml.setup.description";

	@Inject(optional = true)
	@Named(DESCRIPTION)
	private String description;

	@PropConf(
			usage = "The type of interpolation used",
			example = "none|linear|polynomial|cubic|spline"
	)
	public static final String INTERPOLATION = "portal.wiseml.setup.interpolation";

	@Inject(optional = true)
	@Named(INTERPOLATION)
	private String interpolation;

	@PropConf(usage = "The latitude of the testbeds origin")
	public static final String ORIGIN_LATITUDE = "portal.wiseml.setup.origin.latitude";

	@Inject(optional = true)
	@Named(ORIGIN_LATITUDE)
	private Float originLatitude;

	@PropConf(usage = "The longitude of the testbeds origin")
	public static final String ORIGIN_LONGITUDE = "portal.wiseml.setup.origin.longitude";

	@Inject(optional = true)
	@Named(ORIGIN_LONGITUDE)
	private Float originLongitude;

	@PropConf(usage = "The x coordinate of the testbeds origin")
	public static final String ORIGIN_X = "portal.wiseml.setup.origin.x";

	@Inject(optional = true)
	@Named(ORIGIN_X)
	private Float originX;

	@PropConf(usage = "The y coordinate of the testbeds origin")
	public static final String ORIGIN_Y = "portal.wiseml.setup.origin.y";

	@Inject(optional = true)
	@Named(ORIGIN_Y)
	private Float originY;

	@PropConf(usage = "The z coordinate of the testbeds origin")
	public static final String ORIGIN_Z = "portal.wiseml.setup.origin.z";

	@Inject(optional = true)
	@Named(ORIGIN_Z)
	private Float originZ;

	@PropConf(usage = "The phi angle of the testbeds origin")
	public static final String ORIGIN_PHI = "portal.wiseml.setup.origin.phi";

	@Inject(optional = true)
	@Named(ORIGIN_PHI)
	private Float originPhi;

	@PropConf(usage = "The theta angle of the testbeds origin")
	public static final String ORIGIN_THETA = "portal.wiseml.setup.origin.theta";

	@Inject(optional = true)
	@Named(ORIGIN_THETA)
	private Float originTheta;

	@PropConf(usage = "The rho angle of the testbeds origin")
	public static final String ORIGIN_RHO = "portal.wiseml.setup.origin.rho";

	@Inject(optional = true)
	@Named(ORIGIN_RHO)
	private Float originRho;

	@Nullable
	public String getDescription() {
		return description;
	}

	@Nullable
	public Float getOriginPhi() {
		return originPhi;
	}

	@Nullable
	public Float getOriginTheta() {
		return originTheta;
	}

	@Nullable
	public Float getOriginX() {
		return originX;
	}

	@Nullable
	public Float getOriginY() {
		return originY;
	}

	@Nullable
	public Float getOriginZ() {
		return originZ;
	}

	@Nullable
	public String getInterpolation() {
		return interpolation;
	}

	@Nullable
	public Float getOriginLatitude() {
		return originLatitude;
	}

	@Nullable
	public Float getOriginLongitude() {
		return originLongitude;
	}

	@Nullable
	public Float getOriginRho() {
		return originRho;
	}
}
