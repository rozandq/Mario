/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.r2d2.mains;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.r2d2.controller.MyPlanner;
import org.r2d2.sensors.Camera;

import fr.uga.pddl4j.encoding.CodedProblem;

/**
 *
 * @author Quentin
 */
public class Mario {

    /**
     * @param args the command line arguments
     */
	public static void main(String[] args) {
		try {
			MyPlanner planner = new MyPlanner();
			List<String> plan = planner.searchPlan();
			System.out.println(plan);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
}
