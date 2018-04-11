package org.r2d2.sensors;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageWriteParam;

import lejos.hardware.Button;

public class Camera {
	private DatagramSocket dsocket;
	private DatagramPacket packet;
	private byte[] buffer;
	
	public Camera() {
		try {
	    	  
	    	  int port = 8888;
		      
		      // Create a socket to listen on the port.
		      this.dsocket = new DatagramSocket(port);
		
		      // Create a buffer to read datagrams into. If a\
		      // packet is larger than this buffer, the\
		      // excess will simply be discarded!\
		      this.buffer = new byte[2048];
		
			  // Create a packet to receive data into the buffer\
		      this.packet = new DatagramPacket(buffer, buffer.length);
		      this.getPalets();
		}
	    catch (Exception e){
	      System.err.println(e);
	    }
	}
	
	public Integer[][] getPalets() {
		try {
			// Now loop forever, waiting to receive packets and printing them.\
			// Wait to receive a datagram\
			dsocket.receive(packet);
			// Convert the contents to a string, and display them\
			String msg = new String(buffer, 0, packet.getLength());
			//System.out.println(packet.getAddress().getHostName() + ": "\
			//    + msg);\
			String[] palets = msg.split("\\n");
			Integer[][] paletscoord = new Integer[9][2];
			for (int i = 0; i < palets.length; i++) {
				String[] coord = palets[i].split(";");
				int x = Integer.parseInt(coord[1]);
				int y = Integer.parseInt(coord[2]);
				paletscoord[i][0] = x;
				paletscoord[i][1] = y;
				//System.out.println(Integer.toString(x) + " / " + Integer.toString(y) );
			}

			// Reset the length of the packet before reusing it.\
			/*for(Integer[] p : paletscoord) {
				System.out.println(p[0] + " - " + p[1]);
			}*/
			packet.setLength(buffer.length);
			
			//Integer[][] newpalets = Camera.lensDistortionCorrection(paletscoord);			
			
			return paletscoord;
		} catch (Exception e){
			System.err.println(e);
			return null;
		}
	}	
	
	public static List<Integer> getDistances(Integer[] mario, Integer[][] palets) {
		List<Integer> distances = new ArrayList<>();;
		int i = 0;
		for(Integer[] p : palets) {
			if(p != null) {
				distances.add((int) Math.sqrt(Math.pow(p[0] - mario[0], 2) + Math.pow(p[1] - mario[1], 2 )));
				System.out.println(distances.get(i));
				i++;
			}
			
		}
		return distances;
	}
	
	public static Integer[][] lensDistortionCorrection(Integer[][] palets){
		double strength = 1.5;
		
		float halfwidth = (float) 320. / 2;
		float halfheight = (float) 220. / 2;
		
		float correctionRadius = (float) (Math.sqrt(Math.pow(220., 2) + Math.pow(320., 2)) / (float) strength);
		
		for(Integer[] p : palets) {
			System.out.println("x: " + p[1] + " - y: " + p[0]);
			float newx = p[1] - halfwidth;
			float newy = p[0] - halfheight;
			
			float distance = (float) Math.sqrt((float) Math.pow((float) newx, 2) + (float) Math.pow((float) newx, 2));
			float r = distance / correctionRadius;
			
			float theta = 0;
			if(r == 0) {
				theta = 1;
			} else {
				theta = (float) Math.atan(r) / r;
			}
			
			p[1] = (int) (halfwidth + theta * newx);
			p[0] = (int) (halfheight + theta * newy);
			System.out.println("newx: " + p[1] + " - newy: " + p[0]);
		}
				
		return palets;
	}
}