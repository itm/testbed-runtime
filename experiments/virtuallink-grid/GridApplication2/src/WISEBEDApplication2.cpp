/************************************************************************
 ** This file is part of the the iSense project.
 ** Copyright (C) 2006 coalesenses GmbH (http://www.coalesenses.com)
 ** ALL RIGHTS RESERVED.
 ************************************************************************/

#if not defined (ISENSE_MSP430)
	#define WITHEXTENDEDDATA
#endif

//#define DEBUG

#define PIR_TIMEOUT 42
#define ACC_TIMEOUT 53
#define FFD 64

#include <isense/application.h>
#include <isense/os.h>
#include <isense/task.h>
#include <isense/timeout_handler.h>
#include <isense/uart.h>
#include <isense/dispatcher.h>
#include <isense/radio.h>
#include <isense/hardware_radio.h>
#include <isense/button_handler.h>

//#include <isense/util/ishell_interpreter.h>

#include <isense/modules/gateway_module/gateway_module.h>
#include <isense/modules/pacemate_module/pacemate_module.h>
#include <isense/modules/telosb_module/telosb_module.h>
#include <isense/modules/environment_module/environment_module.h>
#include <isense/modules/environment_module/temp_sensor.h>
#include <isense/modules/environment_module/light_sensor.h>
#include <isense/modules/security_module/lis_accelerometer.h>
#include <isense/modules/security_module/pir_sensor.h>

#include <isense/util/spyglass/spyglass_packet.h>
#include <isense/util/spyglass/spyglass_builder.h>

#include "external_interface/isense/isense_os.h"
#include "external_interface/isense/isense_radio.h"
#include "external_interface/isense/isense_timer.h"
#include "external_interface/isense/isense_debug.h"
#include "external_interface/isense/isense_com_uart.h"

#ifdef WITHEXTENDEDDATA
	#include "util/wisebed_node_api/virtual_radio.h"
#else
	#include "util/wisebed_node_api/virtual_radio_old.h"
#endif

typedef wiselib::iSenseOsModel WiselibOs;
typedef wiselib::iSenseSerialComUartModel<WiselibOs> WiselibUart;

#ifdef WITHEXTENDEDDATA
	typedef wiselib::iSenseExtendediSenseStyleTxRadioModel<WiselibOs> ExtendedRadio;
	typedef wiselib::VirtualRadioModel<WiselibOs, ExtendedRadio, WiselibUart> VirtualRadio;
#else
	typedef wiselib::iSenseRadioModel<WiselibOs> WiselibRadio;
	typedef wiselib::VirtualRadioModel_old<WiselibOs, WiselibRadio, WiselibUart> VirtualRadio;
#endif

using namespace isense;

class WISEBEDApplication :
	public isense::Application
	,public isense::Task
	,public isense::TimeoutHandler
	,public isense::UartPacketHandler
	,public isense::Sender
	,public isense::Receiver
	,public isense::ButtonHandler
#if defined (ISENSE_JENNIC_JN513xR1)
	,public isense::BufferDataHandler
	,public isense::SensorHandler
#endif
{
public:
	WISEBEDApplication(isense::Os& _os);

	void boot();

	///From isense::Task
	virtual void execute( void* userdata ) ;

	///From isense::TimeoutHandler
	virtual void timeout( void* userdata ) ;

	///From isense::UartPacketHandler
	virtual void handle_uart_packet(uint8 type , uint8* buf, uint8 length);

	///From isense::ButtonHandler
	virtual void button_down( uint8 button );

	virtual void receive (uint8 len, const uint8 * buf, ISENSE_RADIO_ADDR_TYPE src_addr, ISENSE_RADIO_ADDR_TYPE dest_addr, uint16 signal_strength, uint16 signal_quality, uint8 sequence_no, uint8 interface, Time rx_time) ;

#ifdef WITHEXTENDEDDATA
	void receive_message( uint16 src_addr, uint8 len, uint8 *buf, uint8 rssi, uint8 lqi);
#else
	void receive_message( uint16 src_addr, uint8 len, uint8 *buf);
#endif

	void send_message(uint8 sequence);

	///From isense::Sender
	virtual void confirm (uint8 state, uint8 tries, isense::Time time);
#if defined (ISENSE_JENNIC_JN513xR1)
	// inherited from BufferDataHandler, called when
	// accelerometer data is available
	virtual void handle_buffer_data( BufferData* buf_data );
#endif
#if defined (ISENSE_JENNIC_JN513xR1)
	// inherited from SensorHandler, called when a
	// passive infrared sensor event occurs
	virtual void handle_sensor();
#endif
	void init_sensors();

	void read_sensors();

  //IShellInterpreter* isi_;

  SpyglassBuilder* spyglass_;

#if defined (ISENSE_MSP430)
	TelosbModule *telos_;
#endif

#if defined (ISENSE_JENNIC_JN513xR1)
	GatewayModule* gwm_;

	// pointer to the accelerometer
	EnvironmentModule* em_;

	// pointer to the accelerometer
	LisAccelerometer* acc_;

	// pointer to the passive infrared (PIR) sensor
	PirSensor* pir_;
#endif

	int16 temp_;
	uint32 lux_;

	uint8 counter_;
	uint8 last_sequence_;

	uint8 pir_timeout_;
	bool pir_active_;
	uint8 acc_timeout_;
	bool acc_active_;

	bool em_active_;

	uint8 count;

	uint8 timeout_id_;
	bool app_running_;

private:
#ifdef WITHEXTENDEDDATA
	ExtendedRadio radio_;
#else
	WiselibRadio radio_;
#endif
	WiselibUart uart_;
	WiselibOs::Debug debug_;

	VirtualRadio v_radio_;
};

WISEBEDApplication::WISEBEDApplication(isense::Os &_os)
: Application(_os)
#if defined (ISENSE_MSP430)
	,telos_(new TelosbModule(os()))
#endif
#if defined (ISENSE_JENNIC_JN513xR1)
	,gwm_(new GatewayModule(os()))
	,em_(new EnvironmentModule(os()))
	//,acc_(new LisAccelerometer(os()))
	,pir_(new PirSensor(os()))
#endif
    ,radio_( os() )
    ,uart_( os() )
    ,debug_( os() )
{
	v_radio_.init(radio_, uart_, debug_);
	v_radio_.enable_radio();
	v_radio_.reg_recv_callback<WISEBEDApplication, &WISEBEDApplication::receive_message>( this );

	counter_ = 0;
	last_sequence_ = 0;

	temp_ = 0;
	lux_ = 0;

	timeout_id_ = 0;
	app_running_ = false;

	pir_timeout_ = 0;
	pir_active_ = false;
	acc_timeout_ = 0;
	acc_active_ = false;

	count = 0;

	em_active_ = true;

	//isi_ = new IShellInterpreter(os());
	//isi_->enable_seraerial();

	spyglass_ = new SpyglassBuilder(os());

	uint8 uartchannel = 0;
#if defined (ISENSE_MSP430)
	uartchannel = 1;
#endif

	os().uart(uartchannel).enable_interrupt(true);
	if (os().uart(uartchannel).set_packet_handler(isense::Uart::MESSAGE_TYPE_CUSTOM_IN_2, this) == true)
	{
#ifdef DEBUG
		os().debug("# uart handler ok");
#endif
	}

	os().allow_sleep(false);

	//os().dispatcher().add_receiver(this);

	os().radio().hardware_radio().set_channel(12);
	os().radio().hardware_radio().set_power(-30);

	gwm_->set_button_handler(this);
}

void WISEBEDApplication::boot()
{
#ifdef Debug
	os().debug("boot");
#endif
	#if defined (ISENSE_PACEMATE)
		os_.pacemate_module().clear_screen();
		os_.pacemate_module().write(3,3,"Pacemate WISEBED App");
		os_.pacemate_module().update_display();
		os().debug("boot Pacemate node %i",os().id());
	#endif
	#if defined (ISENSE_MSP430)
		os().debug("boot TelosB node %x",os().id());
	#endif
	#if defined (ISENSE_JENNIC_JN513xR1)
		os().debug("boot iSense node %x",os().id());
	#endif

	init_sensors();

	//timeout_id_ = os().add_timeout_in(Time(10,0), this, NULL);
}

//----------------------------------------------------------------------------
void WISEBEDApplication::timeout( void* userdata )
{
	os().add_task(this, userdata);
}

void WISEBEDApplication::execute( void* userdata )
{
	if (userdata == (void*)PIR_TIMEOUT)
	{
		os().debug("wiseml;4;%x;%i",os().id(), 0);
		spyglass_->paintNodeWithPIR(os().id(), 0);
	}
	else if (userdata == (void*)ACC_TIMEOUT)
	{
		os().debug("wiseml;5;%x;%i;%i;%i",os().id(), 0, 0, 0);
		spyglass_->paintNodeWithACC(os().id(), 0, 0, 0, 0);
	}
	else
	{
	#ifdef DEBUG
		#if defined (ISENSE_PACEMATE)
			os().debug("exe Pacemate node %i at time %i,%i",os().id(), os().time().sec_, os().time().ms_);
		#endif
		#if defined (ISENSE_MSP430)
			os().debug("exe TelosB node %x at time %i,%i",os().id(), os().time().sec_, os().time().ms_);
		#endif
		#if defined (ISENSE_JENNIC_JN513xR1)
			os().debug("exe iSense node %x at time %i,%i",os().id(), os().time().sec_, os().time().ms_);
		#endif
	#endif

		read_sensors();

		send_message(0);

		timeout_id_ = os().add_task_in(Time(60,0), this, NULL);
	}
}

void WISEBEDApplication::button_down( uint8 button_no )
	{
		os().debug("Button %d pressed.", button_no);
		send_message(counter_ ++);
	}

void WISEBEDApplication::init_sensors()
{
	#if defined (ISENSE_PACEMATE)

	#endif
	#if defined (ISENSE_MSP430)
		telos_->init();
	#endif
	#if defined (ISENSE_JENNIC_JN513xR1)

			if (em_->temp_sensor() != NULL)
			{
				em_->temp_sensor()->enable();
			} else
				os().fatal("Could not allocate temp sensor");

			em_->enable(true);

			temp_ = em_->temp_sensor()->temperature();

			if (temp_ == -127 )	// node with PIR and ACC
			{
				em_active_ = false;
			}

			if(em_active_ == true)
			{
				if (em_->light_sensor() != NULL)
				{
					em_->light_sensor()->enable();
				} else
					os().fatal("Could not allocate light sensor");
			}
			else
			{
				acc_ = new LisAccelerometer(os());

				if ((acc_ != NULL) && (pir_!= NULL))
				{
					acc_->set_mode(MODE_THRESHOLD);
					acc_->set_threshold(100);
					acc_->set_handler(this);
					acc_->enable();

					pir_->set_sensor_handler(this);
					pir_->set_pir_sensor_int_interval( 2000 );
					pir_->enable();
				}
			}
	#endif
}

void WISEBEDApplication::read_sensors()
{
	#if defined (ISENSE_PACEMATE)

	#endif
	#if defined (ISENSE_MSP430)
		os().debug("wiseml;6;%x;%i",os().id(), telos_->humidity());
		uint16 temp = telos_->temperature();
		uint16 temprest = temp % 10;
		os().debug("wiseml;1;%x;%i,%i",os().id(), temp / 10, temprest);
		os().debug("wiseml;2;%x;%i",os().id(), telos_->light());
		os().debug("wiseml;3;%x;%i",os().id(), telos_->infrared());
		spyglass_->paintNodeWithTemp(os().id(), temp);
		spyglass_->paintNodeWithLight(os().id(), telos_->light());
		spyglass_->paintNodeWithIrda(os().id(), telos_->infrared());
	#endif
	#if defined (ISENSE_JENNIC_JN513xR1)
		if (em_active_ == true)
		{
			temp_ = em_->temp_sensor()->temperature();
			lux_ = em_->light_sensor()->luminance();
			os().debug("wiseml;1;%x;%i",os().id(), temp_);
			os().debug("wiseml;2;%x;%d",os().id(), lux_);
			spyglass_->paintNodeWithTemp(os().id(), temp_);
			spyglass_->paintNodeWithLight(os().id(), lux_);
		}
		else
		{
		}
	#endif
}
// --------------------------------------------------------------------------

void WISEBEDApplication::send_message(uint8 sequence)
{
#ifdef DEBUG
	os().debug("Sending message to the broadcast address");
#endif
	uint8 buf[5];
	buf[0] = 151;
	buf[1] = 0;//sequence ++;
	buf[2] = 2;
	buf[3] = 3;
	buf[4] = 4;
	v_radio_.send(0xffff, 5, buf);
	//os().radio().send(0xffff, 5, buf, 0, this );
}

void WISEBEDApplication::confirm (uint8 state, uint8 tries, isense::Time time)
{
	//os().debug("Confirm sending message with state=%x", state);
}

void WISEBEDApplication::receive (uint8 len, const uint8 * buf, ISENSE_RADIO_ADDR_TYPE src_addr, ISENSE_RADIO_ADDR_TYPE dest_addr, uint16 signal_strength, uint16 signal_quality, uint8 sequence_no, uint8 interface, Time rx_time)
{
    //os().debug("WISEBEDDemoApp::Received message from %x", src_addr);
}

#ifdef WITHEXTENDEDDATA
void WISEBEDApplication::receive_message( uint16 src_addr, uint8 len, uint8 *buf, uint8 rssi, uint8 lqi)
{
#ifdef DEBUG
	os().debug("WISEBEDDemoApp::Received message via vr from %x with rssi %i and lqi %i", src_addr, rssi, lqi);
#endif
	if ((len==5)&&(buf[0]==151))
	{
		os().debug("wiseml;0;%x;%x;%i;%i", src_addr, os().id(), rssi, lqi);
		spyglass_->paintLink(os().id(), src_addr, lqi);
	}
	last_sequence_ = buf[1];
}
#else
void WISEBEDApplication::receive_message( uint16 src_addr, uint8 len, uint8 *buf)
{
#ifdef DEBUG
	os().debug("WISEBEDDemoApp::Received message via vr from %x with rssi %i and lqi %i", src_addr, 0,0);
#endif

	if ((len==5)&&(buf[0]==151))
	{
		os().debug("wiseml;0;%x;%x;%i;%i", src_addr, os().id(), 0,0);
		spyglass_->paintLink(os().id(), src_addr, 0);
	}
}
#endif
//--------------------------------------------------------------
#if defined (ISENSE_JENNIC_JN513xR1)
void WISEBEDApplication::handle_buffer_data( BufferData* buf_data )
{
	if(app_running_ == true)
	{
		int16 x = 0;
		int16 y = 0;
		int16 z = 0;
		int16 mean[3] = {0,0,0};
		for (uint8 i=0; i<buf_data->count; i++)
		{
			mean[0] += buf_data->buf[i*(buf_data->dim)+0];
			mean[1] += buf_data->buf[i*(buf_data->dim)+1];
			mean[2] += buf_data->buf[i*(buf_data->dim)+2];
		}
		x = (x + (mean[0] / buf_data->count)) / 2;
		y = (y + (mean[1] / buf_data->count)) / 2;
		z = (z + (mean[2] / buf_data->count)) / 2;

		if (x <= 0)
			x = x * (-1);
		if (y <= 0)
			y = y * (-1);
		if (z <= 0)
			z = z * (-1);

		os().debug("wiseml;5;%x;%i;%i;%i",os().id(), x, y, z);
		spyglass_->paintNodeWithACC(os().id(), x, y, z, x+y+z);

		if (acc_active_ == true)
				os().remove_timeout(acc_timeout_, this);
		acc_timeout_ = os().add_timeout_in(5000, this, (void*)ACC_TIMEOUT);
		acc_active_ = true;
	}
	//return from continuous mode to threshold mode
	acc_->set_mode(MODE_THRESHOLD);

}
#endif

#if defined (ISENSE_JENNIC_JN513xR1)
void WISEBEDApplication::handle_sensor()
{
	if (app_running_ == true)
	{
		os().debug("wiseml;4;%x;%i",os().id(), 1);
		spyglass_->paintNodeWithPIR(os().id(), 1);

		if (pir_active_ == true)
			os().remove_timeout(pir_timeout_, this);
		pir_timeout_ = os().add_timeout_in(5000, this, (void*)PIR_TIMEOUT);
		pir_active_ = true;
	}
}
#endif

//--------------------------------------------------------------
void WISEBEDApplication::handle_uart_packet(uint8 type , uint8* buf, uint8 length)
{
	// from iShell
	if (type == 0x0b)
	{
#ifdef DEBUG
		os().debug("uart %x + %i", type, length);
		for (int i = 0; i < length; i++)
			os().debug("uart %i %x",i, buf[i]);
#endif
		if (buf [0] == 0)
		{
			app_running_ = false;
			os().remove_timeout(timeout_id_, this);		// stop
		}
		else if (buf[0] == 1)
		{
			app_running_ = true;
			os().remove_timeout(timeout_id_, this);  // if there is one running already
			timeout_id_ = os().add_timeout_in(Time(1,0), this, NULL);	// start
		}
		else if (buf[0] == 2)
		{
			int16 x = (buf[1] & 0xff) << 8 | (buf[2] & 0xff);	// set the given coords
			int16 y = (buf[3] & 0xff) << 8 | (buf[4] & 0xff);
			int16 z = (buf[5] & 0xff) << 8 | (buf[6] & 0xff);
			os().debug("node coord %i %i %i",x,y,z);
			spyglass_->setCoords(x,y,z);
		}
	}

}

isense::Application* application_factory(isense::Os &os)
{
	return new WISEBEDApplication(os);
}

/*-----------------------------------------------------------------------
* Source  $Source: $
* Version $Revision: 1.24 $
* Date    $Date: 2006/10/19 12:37:49 $
*-----------------------------------------------------------------------
* $Log$
*-----------------------------------------------------------------------*/
