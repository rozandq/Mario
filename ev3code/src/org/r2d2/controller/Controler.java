package org.r2d2.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.r2d2.motors.Graber;
import org.r2d2.motors.TImedMotor;
import org.r2d2.motors.Propulsion;
import org.r2d2.sensors.ColorSensor;
import org.r2d2.sensors.PressionSensor;
import org.r2d2.sensors.VisionSensor;
import org.r2d2.utils.R2D2Constants;
import org.r2d2.vue.InputHandler;
import org.r2d2.vue.Screen;

import fr.uga.pddl4j.util.Plan;
import fr.uga.pddl4j.util.SequentialPlan;

import lejos.hardware.Button;
import lejos.robotics.Color;

public class Controler {

	protected ColorSensor color = null;
	protected Propulsion propulsion = null;
	protected Graber graber = null;
	protected PressionSensor pression = null;
	protected VisionSensor vision = null;
	protected Screen screen = null;
	protected InputHandler input = null;
	private float robotPositionX = 0;
	private float robotPositionY = 0;

	private MyPlanner planner;

	private float Xmax; // valeur maximum de x
	private float Ymax; // valeur maximum de y

	/*
	 * Constantes pour que les calculs : robotPositionX = lx * Xmax - cx * dist
	 * robotPositionY = ly * Ymax - cy * dist soient toujours juste quelque soit
	 * l'orientation du nord
	 */
	private int lx; // 0 si le nord est vers le (0,0) de la camera, 1 sinon
	private int cx; // si lx=0, cx=-1 / si lx=1, cx=1
	private int ly; // inverse de lx : 1 si lx =0 / 0 si lx=1
	private int cy; // si ly=0, cy=-1 / si ly=1, cy=1

	private boolean debuteAGauche; // vrai si le robot commence a gauche de
									// l'arene,
									// en se mettant du cote du pied de la
									// camera.

	private ArrayList<TImedMotor> motors = new ArrayList<TImedMotor>();

	public Controler() {

		propulsion = new Propulsion();
		graber = new Graber();
		color = new ColorSensor();
		pression = new PressionSensor();
		vision = new VisionSensor();
		screen = new Screen();
		input = new InputHandler(screen);
		MyPlanner planner = new MyPlanner();
		motors.add(propulsion);
		motors.add(graber);
	}

	/**
	 * Lance le robot. Dans un premier temps, effectue une calibration des
	 * capteurs. Dans un second temps, lance des tests Dans un troisième temps,
	 * démarre la boucle principale du robot pour la persycup
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void start() throws IOException, ClassNotFoundException {

		screen.drawText("Calibration", "Appuyez sur echap ", "pour skipper");
		boolean skip = input.waitOkEscape(Button.ID_ESCAPE);
		if (skip || calibration()) {
			if (!skip) {
				saveCalibration();
			} else {
				loadCalibration();
			}
		}

		screen.drawText("Lancer", "Appuyez sur OK si le", "robot est à gauche",
				"Appuyez sur tout autre", "il est à droite");
		boolean gauche = input.waitOkEscape(Button.ID_ENTER);
		if (gauche) {
			// 100 0
			robotPositionX = 100;
			robotPositionY = 50;
			debuteAGauche = true;
			lx = 0;
			cx = -1;
			ly = 1;
			cy = 1;
		} else {
			robotPositionX = 100;
			robotPositionY = 300;
			debuteAGauche = false;
			lx = 1;
			cx = 1;
			ly = 0;
			cy = -1;
		}

		jeu();
		cleanUp();
	}

	/**
	 * Charge la calibration du fichier de configuration si elle existe
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void loadCalibration() throws FileNotFoundException, IOException,
			ClassNotFoundException {
		File file = new File("calibration");
		if (file.exists()) {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					file));
			color.setCalibration((float[][]) ois.readObject());
			graber.setOpenTime((long) ois.readObject());
			ois.close();
		}
	}

	/**
	 * Sauvegarde la calibration
	 * 
	 * @throws IOException
	 */
	private void saveCalibration() throws IOException {
		screen.drawText("Sauvegarde", "Appuyez sur le bouton central ",
				"pour valider id", "Echap pour ne pas sauver");
		if (input.waitOkEscape(Button.ID_ENTER)) {
			File file = new File("calibration");
			if (!file.exists()) {
				file.createNewFile();
			} else {
				file.delete();
				file.createNewFile();
			}
			ObjectOutputStream str = new ObjectOutputStream(
					new FileOutputStream(file));
			str.writeObject(color.getCalibration());
			str.writeObject(graber.getOpenTime());
			str.flush();
			str.close();
		}
	}

	/**
	 * Effectue l'ensemble des actions nécessaires à l'extinction du programme
	 */
	private void cleanUp() {
		if (!graber.isOpen()) {
			graber.open();
			while (graber.isRunning()) {
				graber.checkState();
			}
		}
		propulsion.runFor(500, true);
		while (propulsion.isRunning()) {
			propulsion.checkState();
		}
		color.lightOff();
	}

	/**
	 * Lance les tests du robot, peut être desactivé pour la persy cup
	 */
	private void runTests() {
		SystemTest.grabberTest(this);
	}

	private void actionLoop() {
		final File domain = new File("pddl/blocksworld/domainArene.pddl");

		// Lecture de la position des palets
		int[][] matricePalets = new int[9][2];
		int i = 0;
		while (i < 9) {
			matricePalets[i][1] = 0; // Lire valeur x d'un palet
			matricePalets[i][1] = 0; // Lire valeur y d'un palet
			i++;
		}

		// Ecriture de la matrice dans un fichier
		i = 0;
		File f = new File("fichierProbleme.txt");
		String text = "";
		while (i > 9) {
			text = text + matricePalets[i][1] + " " + matricePalets[i][2]
					+ "\n";
		}

		try (PrintWriter out = new PrintWriter(f)) {
			out.println(text);
		} catch (Exception e) {
			System.err.println("Fichier non trouve");
		}

	}

	/**
	 * Lance la boucle de jeu principale
	 * 
	 * Toutes les opérations dans la boucle principale doivent être le plus
	 * atomique possible. Cette boucle doit s'executer très rapidement.
	 */

	enum States {
		firstMove, step2, step22, playStart, isCatching, needToRelease, isReleasing, needToSeek, isSeeking, needToGrab, isGrabing, needToRotateEast, isRotatingToEast, needToRotateWest, isRotatingToWest, needToGoBackHome, isRunningBackHome, needToResetInitialSeekOrientation, isResetingInitialSeekOrientation, needToTurnBackToGoBackHome, isTurningBackToGoBackHome, needToOrientateNorthToRelease, isOrientatingNorthToRealease, isAjustingBackHome, isGoingToOrientateN
	}

	private void mainLoop(boolean initLeft) {
		States state = States.firstMove;
		boolean run = true;
		boolean unique = true;
		boolean unique2 = true;
		float searchPik = R2D2Constants.INIT_SEARCH_PIK_VALUE;
		boolean isAtWhiteLine = false;
		int nbSeek = R2D2Constants.INIT_NB_SEEK;
		boolean seekLeft = initLeft;
		// Boucle de jeu
		while (run) {
			/*
			 * - Quand on part chercher un palet, on mesure le temps de trajet -
			 * Quand on fait le demi tour on parcours ce même temps de trajet -
			 * Si on croise une ligne noire vers la fin du temps de trajet
			 * S'orienter au nord vérifier pendant l'orientation la présence
			 * d'une ligne blanche si on voit une ligne blanche alors le
			 * prochain état sera arrivé à la maison sinon le prochain état sera
			 * aller à la maison.
			 */
			try {
				for (TImedMotor m : motors) {
					m.checkState();
				}
				switch (state) {
				/*
				 * Routine de démarrage du robot : Attraper un palet Emmener le
				 * palet dans le but adverse les roues à cheval sur la ligne
				 * noire. Et passer dans l'état
				 * needToResetInitialSeekOrientation
				 */
				case firstMove:
					propulsion.run(true);
					state = States.playStart;
					break;
				case playStart:
					while (propulsion.isRunning()) {
						if (pression.isPressed()) {
							propulsion.stopMoving();
							graber.close();
						}
					}
					propulsion.rotate(R2D2Constants.ANGLE_START, seekLeft,
							false);
					while (propulsion.isRunning() || graber.isRunning()) {
						propulsion.checkState();
						graber.checkState();
						if (input.escapePressed())
							return;
					}
					propulsion.run(true);
					while (propulsion.isRunning()) {
						propulsion.checkState();
						if (input.escapePressed())
							return;
						if (color.getCurrentColor() == Color.WHITE) {
							propulsion.stopMoving();
						}
					}
					graber.open();
					while (graber.isRunning()) {
						graber.checkState();
						if (input.escapePressed())
							return;
					}
					propulsion.runFor(R2D2Constants.QUARTER_SECOND, false);
					while (propulsion.isRunning()) {
						propulsion.checkState();
						if (input.escapePressed())
							return;
					}
					propulsion.halfTurn(seekLeft);
					while (propulsion.isRunning()) {
						propulsion.checkState();
						if (input.escapePressed())
							return;
					}
					propulsion.run(true);
					while (propulsion.isRunning()) {
						propulsion.checkState();
						if (input.escapePressed())
							return;
						if (color.getCurrentColor() == Color.BLACK) {
							propulsion.stopMoving();
						}
					}
					/*
					 * propulsion.orientateSouth(seekLeft);
					 * while(propulsion.isRunning()){ propulsion.checkState();
					 * if(input.escapePressed()) return; } state =
					 * States.needToGrab;
					 */
					state = States.needToSeek;
					break;
				/*
				 * Le bsoin de chercher un objet nécessite d'avoir le robot
				 * orienté face à l'ouest du terrain. Le nord étant face au camp
				 * adverse Le robot va lancer une rotation de 180° en cherchant
				 * si un pic de distances inférieure à 70cm apparait. Dans ce
				 * cas, il fera une recherche du centre de l'objet et ira
				 * l'attraper
				 * 
				 * TODO faire en sorte que le robot n'avance pas pour une durée
				 * indeterminée, mais qu'il avance sur un temps de référence
				 * pour 70 cm de trajet au maximum. Comme ça, si l'objet a été
				 * attrapé pendant ce temps ou à disparu, alors il ne roulera
				 * pas dans le vide pour rien
				 */
				case needToSeek:
					state = States.isSeeking;
					searchPik = R2D2Constants.INIT_SEARCH_PIK_VALUE;
					propulsion.volteFace(seekLeft, R2D2Constants.SEARCH_SPEED);
					isAtWhiteLine = false;
					break;
				case isSeeking:
					float newDist = vision.getRaw()[0];
					// Si la nouvelle distance est inférieure au rayonMaximum et
					// et supérieure au rayon minimum alors
					// on a trouvé un objet à rammaser.
					if (newDist < R2D2Constants.MAX_VISION_RANGE
							&& newDist >= R2D2Constants.MIN_VISION_RANGE) {
						if (searchPik == R2D2Constants.INIT_SEARCH_PIK_VALUE) {
							if (unique2) {
								unique2 = false;
							} else {
								propulsion.stopMoving();
								// TODO, ces 90° peuvent poser problème.
								// Genre, dans le cas où le dernier palet de la
								// recherche
								// a déclenché la recherche du searchPik,
								// du coup on risque de voir le mur.
								// Il serait plus intéressant de faire un rotate
								// west ou east en fonction.
								// Mais bon, on a jamais eu le bug alors ...
								propulsion.rotate(R2D2Constants.QUART_CIRCLE,
										seekLeft,
										R2D2Constants.SLOW_SEARCH_SPEED);
								searchPik = newDist;
							}
						} else {
							if (newDist <= searchPik) {
								searchPik = newDist;
							} else {
								propulsion.stopMoving();
								unique2 = true;
								state = States.needToGrab;
							}
						}
					} else {
						searchPik = R2D2Constants.INIT_SEARCH_PIK_VALUE;
					}
					if (!propulsion.isRunning() && state != States.needToGrab) {
						nbSeek += R2D2Constants.STEPS_PER_STAGE;
						if (nbSeek > 10) {
							run = false;
						}
						state = States.needToOrientateNorthToRelease;
						seekLeft = System.currentTimeMillis() % 2 == 0;
					}
					break;
				/*
				 * Le besoin d'attraper un objet correspond au besoin de rouler
				 * sur l'objet pour l'attraper dans les pinces.
				 */
				case needToGrab:
					propulsion.runFor(R2D2Constants.MAX_GRABING_TIME, true);
					state = States.isGrabing;
					seekLeft = !seekLeft;
					break;
				/*
				 * Le robot est dans l'état isGrabing tant qu'il roule pour
				 * attraper l'objet.
				 */
				case isGrabing:
					// si le temps de roulage est dépassé, s'arrêter aussi
					if (vision.getRaw()[0] < R2D2Constants.COLLISION_DISTANCE
							|| pression.isPressed() || !propulsion.isRunning()) {
						propulsion.stopMoving();
						state = States.isCatching;
						graber.close();
					}
					break;
				/*
				 * Is catching correspond à l'état où le robot est en train
				 * d'attraper l'objet. Cet état s'arrête quand les pinces
				 * arrêtent de tourner, temps fonction de la calibration
				 */
				case isCatching:
					if (!graber.isRunning()) {
						state = States.needToTurnBackToGoBackHome;
					}
					break;
				/*
				 * Ce état demande au robot de rentrer avec un palet. Dans un
				 * premier temps il effectue un demi tour pour repartir sur la
				 * trajectoire d'où il vient
				 */
				case needToTurnBackToGoBackHome:
					propulsion.volteFace(true,
							R2D2Constants.VOLTE_FACE_ROTATION);
					state = States.isTurningBackToGoBackHome;
					break;
				case isTurningBackToGoBackHome:
					if (!propulsion.isRunning()) {
						state = States.needToGoBackHome;
					}
					break;
				/*
				 * Dans un second temps, le robot va aller en ligne droite pour
				 * rentrer. Le temps de trajet aller a été mesuré. Nous
				 * utilisons cette mesure pour "prédire" à peux prêt quand
				 * est-ce que le robot va arriver à destination. Nous allumerons
				 * les capteurs de couleurs dans les environs pour détecter la
				 * présence d'une ligne blanche ou d'une ligne noire et agir en
				 * conséquence.
				 * 
				 * Si une ligne noire est détectée, alors le robot va s'orienter
				 * face au nord et continuer sa route en direction du camp
				 * adverse.
				 * 
				 * Celà permet d'assurer que le robot restera au centre du
				 * terrain.
				 * 
				 * Si une ligne blanche est détectée, alors le robot sait qu'il
				 * est arrivé et l'état isRunningBackHome sera évacué
				 */
				case needToGoBackHome:
					propulsion.run(true);
					state = States.isRunningBackHome;
					break;
				case isRunningBackHome:
					if (!propulsion.isRunning()) {
						state = States.needToOrientateNorthToRelease;
					}
					if (propulsion
							.hasRunXPercentOfLastRun(R2D2Constants.ACTIVATE_SENSOR_AT_PERCENT)) {
						if (color.getCurrentColor() == Color.WHITE) {
							propulsion.stopMoving();
							isAtWhiteLine = true;
							unique = true;
						}
						if (unique && color.getCurrentColor() == Color.BLACK) {
							propulsion.stopMoving();
							unique = false;
							state = States.isAjustingBackHome;
						}
					}
					break;
				/*
				 * Cet état permet de remettre le robot dans la direction du
				 * nord avant de reprendre sa route
				 */
				case isAjustingBackHome:
					if (!propulsion.isRunning()) {
						propulsion.orientateNorth();
						state = States.isGoingToOrientateN;
					}
					break;
				/*
				 * Cet état correspond à l'orientation du robot face au camp
				 * adverse pour continuer sa route.
				 * 
				 * Il y a cependant un cas particulier, dans le cas où quand le
				 * robot tourne, si il voit la couleur blanche, c'est qu'il est
				 * arrivé. Dans ce cas, terminer la rotation dans l'état
				 * isOrientatingNorthToRealease.
				 */
				case isGoingToOrientateN:
					if (color.getCurrentColor() == Color.WHITE) {
						state = States.isOrientatingNorthToRealease;
					}
					if (!propulsion.isRunning()) {
						state = States.needToGoBackHome;
					}
					break;
				/*
				 * Correspond à l'état où le robot s'oriente au nord pour
				 * relâcher l'objet
				 */
				case needToOrientateNorthToRelease:
					state = States.isOrientatingNorthToRealease;
					propulsion.orientateNorth();
					break;
				case isOrientatingNorthToRealease:
					if (!propulsion.isRunning()) {
						if (graber.isClose()) {
							state = States.needToRelease;
						} else {
							state = States.needToResetInitialSeekOrientation;
						}
					}
					break;
				/*
				 * Ce état correspond, au moment où le robot a besoin de déposer
				 * le palet dans le cap adverse.
				 */
				case needToRelease:
					graber.open();
					state = States.isReleasing;
					break;
				case isReleasing:
					if (!graber.isRunning()) {
						state = States.needToResetInitialSeekOrientation;
					}
					break;
				/*
				 * Une fois l'objet rammassé, il faut se remettre en position de
				 * trouver un autre objet. Le robot fait une marcher arrière
				 * d'un certain temps. Puis fera une mise en face de l'ouest
				 */
				case needToResetInitialSeekOrientation:
					state = States.isResetingInitialSeekOrientation;
					if (isAtWhiteLine) {
						propulsion.runFor(R2D2Constants.HALF_SECOND * nbSeek,
								false);
					} else {
						propulsion.runFor(
								R2D2Constants.EMPTY_HANDED_STEP_FORWARD, false);
					}
					break;
				case isResetingInitialSeekOrientation:
					if (!propulsion.isRunning()) {
						if (seekLeft) {
							state = States.needToRotateWest;
						} else {
							state = States.needToRotateEast;
						}
						if (color.getCurrentColor() == Color.WHITE)// fin de
																	// partie
							return;
					}
					break;
				/*
				 * Remet le robot face à l'ouest pour recommencer la recherche.
				 * Le robot doit avoir suffisamment reculé pour être dans une
				 * zone où il y aura des palets à ramasser.
				 */
				case needToRotateWest:
					propulsion.orientateWest();
					state = States.isRotatingToWest;
					break;
				case isRotatingToWest:
					if (!propulsion.isRunning()) {
						state = States.needToSeek;
					}
					break;
				/*
				 * Remet le robot face à l'est pour recommencer la recherche. Le
				 * robot doit avoir suffisamment reculé pour être dans une zone
				 * où il y aura des palets à ramasser.
				 */
				case needToRotateEast:
					propulsion.orientateEast();
					state = States.isRotatingToWest;
					break;
				case isRotatingToEast:
					if (!propulsion.isRunning()) {
						state = States.needToSeek;
					}
					break;
				// Évite la boucle infinie
				}
				if (input.escapePressed())
					run = false;
			} catch (Throwable t) {
				t.printStackTrace();
				run = false;
			}
		}
	}

	/**
	 * S'occupe d'effectuer l'ensemble des calibrations nécessaires au bon
	 * fonctionnement du robot.
	 * 
	 * @return vrai si tout c'est bien passé.
	 */
	private boolean calibration() {
		return calibrationGrabber() && calibrationCouleur();
	}

	private boolean calibrationGrabber() {
		screen.drawText("Calibration",
				"Calibration de la fermeture de la pince",
				"Appuyez sur le bouton central ", "pour continuer");
		if (input.waitOkEscape(Button.ID_ENTER)) {
			screen.drawText("Calibration", "Appuyez sur ok",
					"pour lancer et arrêter");
			input.waitAny();
			graber.startCalibrate(false);
			input.waitAny();
			graber.stopCalibrate(false);
			screen.drawText("Calibration", "Appuyer sur Entree",
					"pour commencer la", "calibration de l'ouverture");
			input.waitAny();
			screen.drawText("Calibration", "Appuyer sur Entree",
					"Quand la pince est ouverte");
			graber.startCalibrate(true);
			input.waitAny();
			graber.stopCalibrate(true);

		} else {
			return false;
		}
		return true;
	}

	/**
	 * Effectue la calibration de la couleur
	 * 
	 * @return renvoie vrai si tout c'est bien passé
	 */
	private boolean calibrationCouleur() {
		screen.drawText("Calibration", "Préparez le robot à la ",
				"calibration des couleurs", "Appuyez sur le bouton central ",
				"pour continuer");
		if (input.waitOkEscape(Button.ID_ENTER)) {
			color.lightOn();
			/*
			 * //calibration gris screen.drawText("Gris",
			 * "Placer le robot sur ","la couleur grise"); input.waitAny();
			 * color.calibrateColor(Color.GRAY);
			 * 
			 * //calibration rouge screen.drawText("Rouge",
			 * "Placer le robot ","sur la couleur rouge"); input.waitAny();
			 * color.calibrateColor(Color.RED);
			 * 
			 * 
			 * //calibration jaune screen.drawText("Jaune",
			 * "Placer le robot sur ","la couleur jaune"); input.waitAny();
			 * color.calibrateColor(Color.YELLOW);
			 * 
			 * //calibration bleue screen.drawText("BLeue",
			 * "Placer le robot sur ","la couleur bleue"); input.waitAny();
			 * color.calibrateColor(Color.BLUE);
			 * 
			 * //calibration vert screen.drawText("Vert",
			 * "Placer le robot ","sur la couleur vert"); input.waitAny();
			 * color.calibrateColor(Color.GREEN);
			 */
			// calibration gris
			screen.drawText("Gris", "Placer le robot sur ", "la couleur grise");
			input.waitAny();
			color.calibrateColor(Color.GRAY);

			// calibration blanc
			screen.drawText("Blanc", "Placer le robot ", "sur la couleur blanc");
			input.waitAny();
			color.calibrateColor(Color.WHITE);

			// calibration noir
			screen.drawText("Noir", "Placer le robot ", "sur la couleur noir");
			input.waitAny();
			color.calibrateColor(Color.BLACK);

			color.lightOff();
			return true;
		}
		return false;
	}

	private void fonctiontestmove() {
		propulsion.runFor(50000, true);
		// propulsion.stopMoving();
	}

	private void fonctiontestrotation() {
		propulsion.rotate(180, false, 20);
		while (propulsion.isRunning()) {
			if (vision.getRaw()[0] < 0.30f) {
				propulsion.stopMoving();
			}
		}
	}

	private void attendAction() {
		while (propulsion.isRunning())
			;
	}

	public void jeu() {
		boolean run = true;
		boolean aGauche = debuteAGauche;

		while (run) {

			List<String> plan = planner.searchPlan();
			for (int i = 0; i < plan.size(); i++) {

				switch (plan.get(i)) {

				case "allerchercherpalet":
					deplacement(50, 100, aGauche);
					boolean aTrouve = balayageZone();
					break;

				case "ramasserpalet":
					if (aTrouve) {
						ramassePalet();
					} else {
						graber.runFor(0, false); // On ferme les pinces autour
													// du palet
						while (graber.isRunning())
							; // On attend que les pinces se soient refermees
					}
					break;

				case "rentrermaison":
					vaDeposePalet();
					savoirOuOnEst();
					break;
				}

			}

			// inversion du coté après le premier palet
			if (aGauche == debuteAGauche) {
				aGauche = !aGauche;
			}

			if (input.escapePressed()) {
				run = false;
			}

		}
	}

	/*
	 * On sait deja que le palet n'est pas exactement sur l'intersection On
	 * s'arrete un peu avant pour le trouver On retourne vrai si on a trouve un
	 * palet, faux sinon
	 */
	public boolean balayageZone() {

		boolean aTrouvePalet = false;
		float searchPik = -1;
		propulsion.rotate(360, false, 20);
		while (propulsion.isRunning()) {
			float newDist = vision.getRaw()[0];
			if (newDist < 0.60f && newDist >= R2D2Constants.MIN_VISION_RANGE) {
				if (searchPik - newDist > 0.20f) {
					propulsion.stopMoving();
					propulsion.rotate(5, false, 20);
					propulsion.stopMoving();
					aTrouvePalet = true;
				} else {
					searchPik = newDist;
				}

			}
		}
		return aTrouvePalet;
	}

	public void ramassePalet() {

		// Voir pour ajuster le temps !+
		propulsion.runFor(R2D2Constants.MAX_GRABING_TIME, true,
				R2D2Constants.MAX_ROTATION_SPEED);

		// On avance jusqu'a detecter le palet par pression ou jusqu'a ce qu'on
		// risque de se prendre un mur
		while (!pression.isPressed() && propulsion.isRunning())
			;

		propulsion.stopMoving(); // On s'arrete
		graber.runFor(0, false);
		// while(graber.isRunning()); // On attend que les pinces se soient
		// refermees

	}

	/*
	 * Pre-Cond : Mario a un palet dans ses pinces Le camps adverse est au nord
	 * : Le sens dans lequel on était orienté audebut de la partie A la fin de
	 * cette fonction : Mario est a sa position "initiale"
	 */
	public void vaDeposePalet() {
		// On oriente le robot vers le camps adverse
		propulsion.orientateNorth();
		while (propulsion.isRunning())
			;
		// On avance jusqu'a etre dans le camps adverse : quand on a depasse une
		// ligne blanche
		propulsion.run(true);

		// On avance jusqu'a trouver la ligne blanche
		while (color.getCurrentColor() != Color.WHITE) {
			// Si on risque une collision
			/*
			 * if(vision.getRaw()[0] < R2D2Constants.COLLISION_DISTANCE){
			 * propulsion.stopMoving(); propulsion.orientateEast();
			 * attendAction(); if(vision.getRaw()[0] > 0.50f){ // Si on a assez
			 * de place propulsion.runFor(1000, true); // On contourne de ce
			 * cote } else { propulsion.orientateWest(); // Sinon on contourne
			 * de l'autre cote attendAction(); propulsion.runFor(1000, true); }
			 * attendAction(); propulsion.orientateNorth(); attendAction();
			 * 
			 * propulsion.run(true); // On reprend notre route }
			 */
			// Si on croise une ligne noire : on se re-oriente
			if (color.getCurrentColor() == Color.BLACK) {
				propulsion.stopMoving();
				propulsion.orientateNorth();
				attendAction();
				propulsion.run(true);
			}
		}

		// On est arrive dans le camp adverse
		propulsion.stopMoving();

		// On depose le palet le plus pres du mur possible
		propulsion.run(true);
		while (vision.getRaw()[0] > R2D2Constants.COLLISION_DISTANCE)
			;
		propulsion.stopMoving();

		graber.runFor(0, true);
		// while(graber.isRunning()); // On attend que les pinces s'ouvrent

	}

	/*
	 * Appeler a la fin de la fonction DeposePalet Le robot est alors le plus
	 * près possible du mur, et vient de déposer un palet
	 */
	public void savoirOuOnEst() {

		// On recule jusqu'à croiser la ligne blanche
		propulsion.run(false);
		while (color.getCurrentColor() != Color.WHITE)
			;
		propulsion.stopMoving();

		// On met a jour la variable globale y
		robotPositionY = ly * Ymax - cy * vision.getRaw()[0];

		// On se tourne a droite et on avance jusqu'a etre a 40 cm du mur
		propulsion.orientateEast();
		attendAction();
		propulsion.run(true);
		while (vision.getRaw()[0] > R2D2Constants.MAX_VISION_RANGE
				- R2D2Constants.MIN_VISION_RANGE)
			;
		propulsion.stopMoving();

		// On met la variable globale x a jour
		robotPositionX = lx * Xmax - cx * vision.getRaw()[0];

	}

	public void deplacement(float destX, float destY,
			boolean faitRechercheAGauche) {

		float d1 = Math.abs(destY - robotPositionY);
		float d2 = Math.abs(destX - robotPositionX);

		// hypothenus
		float h = (float) Math.sqrt(d1 * d1 + d2 * d2);

		// angle a tourner
		float alpha;

		if (faitRechercheAGauche) {
			if (destX < robotPositionX) { // plus de 90°
				alpha = (float) Math.acos(d1 / h);
				alpha += Math.PI / 2;
			} else { // moins de 90°
				alpha = (float) Math.acos(d2 / h);
			}
		} else {
			if (destX > robotPositionX) { // plus de 90°
				alpha = (float) Math.acos(d1 / h);
				alpha += Math.PI / 2;
			} else { // moins de 90°
				alpha = (float) Math.acos(d2 / h);
			}
		}
		alpha = (float) Math.toDegrees(alpha);
		System.out.println(alpha);

		propulsion.rotate(alpha, false, true);
		attendAction();
		double distancereelle = h - 25;
		if (distancereelle < 0) {
			distancereelle = 0;
		}
		propulsion.run(true);
		try {
			Thread.sleep((long) (1000 * ((10 * distancereelle) / 331)));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		propulsion.stopMoving();
	}

}

// * Compte-rendu : pas plus de quelques pages, choix d'implémentation (si on
// fait tourner le planner sur la brique ou sur l'ordi).
// * Bien commenter le code