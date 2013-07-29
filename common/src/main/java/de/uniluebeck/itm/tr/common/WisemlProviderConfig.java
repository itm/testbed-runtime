package de.uniluebeck.itm.tr.common;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;

import javax.annotation.Nullable;

public class WisemlProviderConfig {

	@PropConf(usage = "A human readable description of the testbed")
	public static final String DESCRIPTION = "portal.wiseml.setup.description";

	@Inject
	@Named(DESCRIPTION)
	private String description;

	@PropConf(usage = "The type of coordinate used for the testbed origin")
	public static final String COORDINATE_TYPE = "portal.wiseml.setup.coordinate_type";

	@Inject
	@Named(COORDINATE_TYPE)
	private String coordinateType;

	@PropConf(
			usage = "The type of interpolation used",
			example = "none|linear|polynomial|cubic|spline"
	)
	public static final String INTERPOLATION = "portal.wiseml.setup.interpolation";

	@Inject
	@Named(INTERPOLATION)
	private String interpolation;

	@PropConf(usage = "The x coordinate of the testbeds origin")
	public static final String ORIGIN_X = "portal.wiseml.setup.origin.x";

	@Inject(optional = true)
	@Named(ORIGIN_X)
	private Double originX;

	@PropConf(usage = "The y coordinate of the testbeds origin")
	public static final String ORIGIN_Y = "portal.wiseml.setup.origin.y";

	@Inject(optional = true)
	@Named(ORIGIN_Y)
	private Double originY;

	@PropConf(usage = "The z coordinate of the testbeds origin")
	public static final String ORIGIN_Z = "portal.wiseml.setup.origin.z";

	@Inject(optional = true)
	@Named(ORIGIN_Z)
	private Double originZ;

	@PropConf(usage = "The phi angle of the testbeds origin")
	public static final String ORIGIN_PHI = "portal.wiseml.setup.origin.phi";

	@Inject(optional = true)
	@Named(ORIGIN_PHI)
	private Double originPhi;

	@PropConf(usage = "The theta angle of the testbeds origin")
	public static final String ORIGIN_THETA = "portal.wiseml.setup.origin.theta";

	@Inject(optional = true)
	@Named(ORIGIN_THETA)
	private Double originTheta;

	@Nullable
	public String getCoordinateType() {
		return coordinateType;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	@Nullable
	public Double getOriginPhi() {
		return originPhi;
	}

	@Nullable
	public Double getOriginTheta() {
		return originTheta;
	}

	@Nullable
	public Double getOriginX() {
		return originX;
	}

	@Nullable
	public Double getOriginY() {
		return originY;
	}

	@Nullable
	public Double getOriginZ() {
		return originZ;
	}

	@Nullable
	public String getInterpolation() {
		return interpolation;
	}
}
