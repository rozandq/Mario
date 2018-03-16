(define 
	(domain ARENE)
	(:requirements :strips :typing)
	(:types sommet)
	(:predicates 
		(ingame ?x - sommet)
		(notingame ?x - sommet)
		(succ ?x - sommet ?y - sommet)
		(roboton ?x - sommet)
		(finish)
		)
	(:action allerchercherpalet
		:parameters (?robot - sommet ?x - sommet)
		:precondition (and (roboton ?robot) (ingame ?x) (succ ?robot ?x))
		:effect (and (not (ingame ?x)) (notingame ?x) (finish)))
	(:action move 
		:parameters (?robot - sommet ?x - sommet)
		:precondition (and (roboton ?robot) (notingame ?x) (succ ?robot ?x))
		:effect (and (not (roboton ?robot)) (roboton ?x)))
)

