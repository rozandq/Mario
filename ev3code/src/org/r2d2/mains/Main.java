package org.r2d2.mains;

import org.r2d2.controller.Controler;

import lejos.utility.Delay;

public class Main {

	public static void main(String[] args) {
		Controler controler = new Controler();
		try{
			controler.start();
		}catch(Throwable e){
			e.printStackTrace();
			Delay.msDelay(10000);
		}
		System.exit(0);
	}

}
