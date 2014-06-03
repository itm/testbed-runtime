package de.uniluebeck.itm.tr.common;

public abstract class Constants {

	public static abstract class SOAP_API_V3 {

		public static final String SNAA_CONTEXT_PATH = "/soap/v3/snaa";

		public static final String RS_CONTEXT_PATH = "/soap/v3/rs";

		public static final String SM_CONTEXT_PATH = "/soap/v3/sm";

		public static final String WSN_CONTEXT_PATH_BASE = "/soap/v3/wsn";

	}

	public static abstract class REST_API_V1 {

		public static final String REST_API_CONTEXT_PATH_KEY = "wisegui.rest_api.context_path";

		public static final String REST_API_CONTEXT_PATH_VALUE = "/rest/v1.0";

		public static final String WEBSOCKET_CONTEXT_PATH_KEY = "wisegui.websocket_api.context_path";

		public static final String WEBSOCKET_CONTEXT_PATH_VALUE = "/ws/v1.0";

	}

	public static abstract class DEVICE_DB {

		public static final String DEVICEDB_REST_API_CONTEXT_PATH_KEY = "devicedb.rest_api.context_path";

		public static final String DEVICEDB_REST_API_CONTEXT_PATH_VALUE = "/rest/v1.0/devicedb";

		public static final String DEVICEDB_REST_ADMIN_API_CONTEXT_PATH_KEY = "devicedb.admin_rest_api.context_path";

		public static final String DEVICEDB_REST_ADMIN_API_CONTEXT_PATH_VALUE = "/rest/v1.0/devicedb/admin";

		public static final String DEVICEDB_WEBAPP_CONTEXT_PATH_KEY = "devicedb.webapp.context_path";

		public static final String DEVICEDB_WEBAPP_CONTEXT_PATH_VALUE = "/admin/devicedb";
	}

	public static abstract class SHIRO_SNAA {

		public static final String ADMIN_WEB_APP_CONTEXT_PATH_KEY = "snaa.shiro.admin_web_app_context_path";

		public static final String ADMIN_WEB_APP_CONTEXT_PATH_VALUE = "/admin/shiro-snaa";

		public static final String ADMIN_REST_API_CONTEXT_PATH_KEY = "snaa.shiro.admin_rest_api_context_path";

		public static final String ADMIN_REST_API_CONTEXT_PATH_VALUE = "/admin/shiro-snaa/rest";
	}

	public static abstract class WISEGUI {

		public static final String CONTEXT_PATH_KEY = "wisegui.context_path";

		public static final String CONTEXT_PATH_VALUE = "/";
	}

	public static abstract class USER_REG {

		public static final String WEB_APP_CONTEXT_PATH = "/user_registration";
	}
}
