(define (problem FIND-PALETS)
	(:domain ARENE)
	(:objects P1 P2 P3 P4 P5 P6 P7 P8 P9 START - sommet)
	(:INIT 
		(notingame P1) 
		(notingame P2) 
		(notingame P3) 
		(notingame P4) 
		(notingame P5) 
		(notingame P6) 
		(notingame P7) 
		(notingame P8) 
		(ingame P9) 
		(notingame START)
		(succ P1 P2) 
		(succ P1 P4) 
		(succ P2 P1) 
		(succ P2 P3) 
		(succ P2 P5)
		(succ P3 P2) 
		(succ P3 P6) 
		(succ P4 P1) 
		(succ P4 P5) 
		(succ P4 P7) 
		(succ P5 P2) 
		(succ P5 P4) 
		(succ P5 P6) 
		(succ P5 P8) 
		(succ P6 P3) 
		(succ P6 P5) 
		(succ P6 P9) 
		(succ P7 P4) 
		(succ P7 P8) 
		(succ P8 P5) 
		(succ P8 P7) 
		(succ P8 P9) 
		(succ P9 P6) 
		(succ P9 P8) 
		(roboton START) 
		(succ START P1)) 
	(:goal (finish))
)
