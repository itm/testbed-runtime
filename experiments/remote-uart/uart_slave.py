#!/usr/bin/env python
# -*- coding: utf-8 -*-

import serial
import sys
import time
import datetime
import struct

import pygtk
pygtk.require('2.0')
import gtk
import gobject

DLE, STX, ETX = 0x10, 0x02, 0x03
ECHO_REQUEST = 60

port = '/dev/ttyUSB0'
baudrate = 115200

class State:
	IDLE, RECEIVE, ESCAPE = range(3)

class UARTSlave:
	def __init__(self):
		self.receive_state = State.IDLE
		self.receiving = ''
		
		self.window = gtk.Window(gtk.WINDOW_TOPLEVEL)
		self.window.connect("destroy", self.exit)
		
		box1 = gtk.VBox(False, 0)
		self.window.add(box1)
		box1.show()
		
		sw = gtk.ScrolledWindow()
		sw.set_policy(gtk.POLICY_AUTOMATIC, gtk.POLICY_AUTOMATIC)
		textview = gtk.TextView()
		textbuffer = textview.get_buffer()
		self.textview = textview
		sw.add(textview)
		sw.show()
		textview.show()
		box1.pack_start(sw, 1)
		textview.set_cursor_visible(True)
		textview.set_editable(True)
		
		echo_box = gtk.HBox(False, 0)
		box1.pack_start(echo_box, 0)
		echo_box.show()
		
		reset_button = gtk.Button("Reset")
		reset_button.connect("clicked", self.reset_node)
		reset_button.show()
		echo_box.pack_start(reset_button, 0)
		
		self.echo_address = gtk.Entry()
		self.echo_address.show()
		echo_box.pack_start(self.echo_address, 0)
		
		self.echo_text = gtk.Entry()
		self.echo_text.show()
		echo_box.pack_start(self.echo_text, 1)
		
		echo_button = gtk.Button("Send echo request")
		echo_button.connect("clicked", self.send_echo_request)
		echo_button.show();
		echo_box.pack_start(echo_button, 0)
		
		self.window.show()
		
		self.tag_debug = textbuffer.create_tag(foreground="darkgrey", font="Sans 14")
		self.tag_source = textbuffer.create_tag(foreground="black", font="Sans 20")
		self.tag_message = textbuffer.create_tag(foreground="blue", font="Sans 20")
	
	def reset_node(self, *args, **kws):
		time.sleep(0.5)
		self.tty.setRTS(True)
		self.tty.setDTR(True)
		
		time.sleep(0.5)
		self.tty.setRTS(False)
		self.tty.setDTR(False)

		time.sleep(0.5)
		
		
	def send_echo_request(self, *args, **kws):
		print "sending echo request"
		address = self.echo_address.get_text()
		message = self.echo_text.get_text()
		
		packet = struct.pack('!BBBBBQQx', DLE, STX, 10, ECHO_REQUEST, len(message), int(address), 0)
		packet += message
		packet += struct.pack('!BB', DLE, ETX)
		self.tty.write(packet)
	
	def connect(self, port, baudrate):
		self.tty = serial.Serial(port=port, baudrate=baudrate, bytesize=8,
				parity='N', stopbits=1, timeout=0.01)
		
		# reboot iSense Node
		time.sleep(0.5)
		self.tty.setRTS(True)
		self.tty.setDTR(True)
		
		time.sleep(0.5)
		self.tty.setRTS(False)
		self.tty.setDTR(False)

		time.sleep(0.5)
		
	def exit(self, *args, **kwargs):
		gtk.main_quit()
		
	def receive_message(self, body, source):
		print "recv message: ", source, body, len(body)
		#for m in body:
		#	print "%s (%d)" % (m, ord(m))
		
		body = body.replace('\x00', '')
		
		textbuffer = self.textview.get_buffer()
		end_iter = textbuffer.get_end_iter()
		mark = textbuffer.create_mark(None, end_iter, False)
		textbuffer.insert_with_tags(end_iter, '[%s] %s: ' % (datetime.datetime.now().strftime('%H:%M:%S'), source), self.tag_source)
		
		end_iter = textbuffer.get_end_iter()
		textbuffer.insert_with_tags(end_iter, '%s\n' % body, self.tag_message)
		
		self.textview.scroll_to_mark(mark, 0, True, 0.0, 1.0)
		textbuffer.delete_mark(mark)
		textbuffer.set_modified(True)
		
		
	def receive_debug(self, body):
		print "recv debug: ", body
		
		textbuffer = self.textview.get_buffer()
		end_iter = textbuffer.get_end_iter()
		mark = textbuffer.create_mark(None, end_iter, False)
		
		textbuffer.insert_with_tags(end_iter, 'DEBUG: %s\n' % body, self.tag_debug)
		
		self.textview.scroll_to_mark(mark, 0, True, 0.0, 1.0)
		textbuffer.delete_mark(mark)
		textbuffer.set_modified(True)
		
	def receive_packet(self, data):
		isense_type = ord(data[0])
		
		if isense_type == 105:
			cmd_type = ord(data[1])
			if cmd_type == 60:
				assert len(data) >= 19
				length, destination, source = struct.unpack('!BQQ', data[2:19])
				print "length=", length
				self.receive_message(data[20:], source=source)
				
		elif isense_type == 104:
			self.receive_debug(data[2:])
		
	def receive_byte(self, c):
		#print "recv_byte %s (%d)" % (c, ord(c))
		if self.receive_state == State.ESCAPE:
			if ord(c) == ETX:
				self.receive_packet(self.receiving)
				self.receive_state = State.IDLE
				
			elif ord(c) == STX:
				self.receiving = ''
				self.receive_state = State.RECEIVE
				
			else:
				self.receiving += c
				
		elif ord(c) == DLE:
			self.receive_state = State.ESCAPE
			
		elif self.receive_state == State.RECEIVE:
			self.receiving += c
	
	def refresh(self, *args, **kws):
		while True:
			c = self.tty.read(1)
			if not c: break
			self.receive_byte(c)
			
		return True

if __name__ == '__main__':
	slave = UARTSlave()
	slave.connect(port, baudrate)
	
	gobject.timeout_add(1000, slave.refresh)
	gtk.main()


