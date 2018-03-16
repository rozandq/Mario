/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mario;

import fr.uga.pddl4j.encoding.CodedProblem;
import fr.uga.pddl4j.exceptions.FileException;
import fr.uga.pddl4j.heuristics.relaxation.Heuristic;
import fr.uga.pddl4j.parser.Parser;
import fr.uga.pddl4j.parser.Problem;
import fr.uga.pddl4j.planners.ProblemFactory;
import fr.uga.pddl4j.planners.hsp.HSP;
import fr.uga.pddl4j.util.BitExp;
import fr.uga.pddl4j.util.BitOp;
import fr.uga.pddl4j.util.CondBitExp;
import fr.uga.pddl4j.util.MemoryAgent;
import fr.uga.pddl4j.util.SequentialPlan;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 * @author Quentin
 */
public class Mario {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, FileException {
        String domain = "C:/Users/Quentin/Desktop/PATIA/pddl4j-master/pddl/blocksworld/domainArene.pddl";
        String problem = "C:/Users/Quentin/Desktop/PATIA/pddl4j-master/pddl/blocksworld/pArene.pddl";
        ArrayList<String> sommets = new ArrayList();
        sommets.add("P1");
        sommets.add("P2");
        sommets.add("P3");
        sommets.add("P4");
        sommets.add("P5");
        sommets.add("P6");
        sommets.add("P7");
        sommets.add("P8");
        sommets.add("P9");
        sommets.add("Start");
        
        ProblemFactory factory = new ProblemFactory();
        factory.parse(new File(domain), new File(problem));
        CodedProblem cp = factory.encode();
        
        final HSP planner = new HSP();
        planner.setSaveState(false);

        SequentialPlan plan = planner.search(cp);
        
        for(BitOp op : plan.actions()){
            System.out.println(op.getName() + " from " + sommets.get(op.getValueOfParameter(0)) + " to " + sommets.get(op.getValueOfParameter(1)));
        }
    }
}
