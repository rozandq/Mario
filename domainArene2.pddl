(define 
	(domain ARENE)
	(:requirements :strips :typing)
	(:types palet sommet)
	(:predicates 
		(ingame ?x - palet)
		(notingame ?x - palet)
		(ison ?x - palet ?y - sommet)
		(closer ?x - sommet ?y - sommet)
		(roboton ?x - sommet)
		(hold ?x - palet)
		(finish)
		)
	(:action move 
		:parameters (?robot - sommet ?x - sommet)
		:precondition (and (roboton ?robot) (ingame ?x) (closer ?robot ?x))
		:effect (and (not (roboton ?robot)) (roboton ?x)))
	(:action ramasserpalet
		:parameters (?robot - sommet ?x - palet)
		:precondition (and (roboton ?robot) (ingame ?x) (ison ?x ?robot))
		:effect (and (not (ingame ?x)) (notingame ?x) (hold ?x)))
	(:action rentreralamaison
		:parameters (?robot - sommet ?x - palet))	
)	

