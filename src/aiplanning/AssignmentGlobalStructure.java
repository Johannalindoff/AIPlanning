/**
 * @file AssignmentGlobalStructure.java
 * @author - id19ohn, id19erd, id19llt, id19jlf
 * @date - 2021-10-19
 * @version - v2.0 (based on given code)
 */

package aiplanning;
import java.awt.Point;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import deterministicplanning.models.Plan;
import deterministicplanning.models.WorldModel;
import deterministicplanning.models.pedagogy.ListbasedNongenericWorldModel;
import deterministicplanning.solvers.Planning;
import deterministicplanning.solvers.planningoutcomes.FailedPlanningOutcome;
import deterministicplanning.solvers.planningoutcomes.PlanningOutcome;
import deterministicplanning.solvers.planningoutcomes.SuccessfulPlanningOutcome;
import finitestatemachine.Action;
import finitestatemachine.State;
import markov.impl.PairImpl;
import obstaclemaps.MapDisplayer;
import obstaclemaps.ObstacleMap;
import obstaclemaps.Path;

public class AssignmentGlobalStructure {

	private static boolean startIsFound;
	private enum movement implements Action {STILL, NORTH, EAST, SOUTH, WEST}

	public static void main(String[] args) {
		/**
		 * First step of the processing pipeline: sensing
		 * This step provides the decision system with the right information about the environment.
		 * In this case, this information is: where do we start, where do we end, where are the obstacles.
		 */

		File inputFile = Paths.get(args[0]).toFile();
		ObstacleMap om = generateObstacleMap(inputFile);
		Point start = getStart(inputFile);
		Point goal = getEnd(inputFile);

		// Display map
		MapDisplayer md = MapDisplayer.newInstance(om);
		md.setVisible(true);

		State startState = toState(start);
		State goalState = toState(goal);

		/**
		 * Second step of the processing pipeline: deciding
		 * This step projects the pre-processed sensory input into a decision
		 * structure
		 */

		startIsFound = false;
		WorldModel<State, Action> wm = generateWorldModel(om, goal, start, startState, goalState);

		PlanningOutcome po = Planning.resolve(wm, startState, goalState, 200);

		/**
		 * Third step of the processing pipeline: action
		 * This step turns the outcome of the decision into a concrete action:
		 * either printing that no plan is found or which plan is found.
		 */
		if (po instanceof FailedPlanningOutcome) {
			System.out.println("No plan could be found.");
			return;
		} else {
			Plan<States, Action> plan = ((SuccessfulPlanningOutcome) po).getPlan();
			Path p = planToPath(plan);
			md.setPath(p);
			System.out.println(p);
		}
	}


	private static Path planToPath(Plan<States, Action> plan) {
		List<Path.Direction> movementDirection = new ArrayList<>();

		for(int i = 0; i<plan.getStateActionPairs().size(); i++){
			PairImpl<States, Action> stateActionPair = plan.getStateActionPairs().get(i);
			movementDirection.add(Path.Direction.valueOf(stateActionPair.getRight().toString()));
		}

		return new Path(plan.getStateActionPairs().get(0).getLeft().getPoint(), movementDirection);
	}


	private static State toState(Point p) {

		return new States(p);
	}


	private static ObstacleMap generateObstacleMap(File inputFile) {

		try {

			// Scan the map
			ArrayList<String> omTemp;
			omTemp = new ArrayList<>();
			Scanner scan = new Scanner(inputFile);
			while (scan.hasNextLine()) {
				omTemp.add(scan.nextLine());
			}
			scan.close();

			// Get height
			int height = omTemp.size();

			// Get width
			int width = 0;
			for (int y = 0; y < height; y++) {
				int currentWidth = omTemp.get(y).length();
				if (currentWidth > width) {
					width = currentWidth;
				}
			}

			// Get all obstacles
			Set<Point> obstacles = new HashSet<Point>();
			for (int y = 0; y < height; y++) {
				int omLength = omTemp.get(y).length();
				for (int x = 0; x < omLength; x++) {
					if (omTemp.get(y).charAt(x) == '#') {
						Point op = new Point(x, y);
						obstacles.add(op);
					}
				}
			}

			// Create obstacle map
			return new ObstacleMap(width, height, obstacles);

		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Error, try again");
		}
		return null;
	}


	private static Point getEnd(File inputFile) {

		try {
			// Scan the map
			ArrayList<String> omTemp;
			omTemp = new ArrayList<>();
			Scanner scan = new Scanner(inputFile);

			int whichLine = 0;
			while (scan.hasNextLine()) {
				omTemp.add(scan.nextLine());
				String currentRow = omTemp.get(whichLine);

				int rowLength = currentRow.length();

				// Check each char
				for (int i = 0; i < rowLength; i++) {
					if (currentRow.charAt(i) == '.') {
						return new Point(i, whichLine); //Stop searching when goal is found since there can only be one
					}
				}
				whichLine++;
			}
			scan.close();

		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Error, try again");
		}
		return null;
	}


	private static Point getStart(File inputFile) {

		try {

			// Scan the obstacle map
			ArrayList<String> omTemp;
			omTemp = new ArrayList<>();
			Scanner scan = new Scanner(inputFile);

			// Check each row if it contains the start position
			int whichLine = 0;
			while (scan.hasNextLine()) {
				omTemp.add(scan.nextLine());
				String currentRow = omTemp.get(whichLine);
				if (currentRow.contains("@")) {
					int x = currentRow.indexOf("@");
					int y = whichLine;

					return new Point(x, y); // Stop searching when start is found since there can only be one
				}
				whichLine++;
			}
			scan.close();

		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Error, try again");
		}
		return null;
	}


	private static WorldModel<State, Action> generateWorldModel(ObstacleMap om, Point goal, Point start, State startState, State goalState) {

		ListbasedNongenericWorldModel model = new ListbasedNongenericWorldModel();

		States currentState = new States(goal); // Make a state of the goal point
		Set<Point> listOfObstacles = om.getObstacles(); // Create a list of all obstacles
		Stack<States> unchecked = new Stack<States>(); // Stack of states (positions) that has to be "checked"
		Stack<Point> visited = new Stack<Point>(); // Stack of states (positions) that has been "checked"
		ArrayList<States> stateObjects = new ArrayList<>(); // List of state objects that already has been created

		unchecked.push(currentState); // Add the goal state to the list of states that should be "checked"
		stateObjects.add(currentState); // Declare that the goal state already has been created
		States availableState = null; // Create a state variable that keeps track of the "available" state next to currentState

		model.addTransition(goalState,movement.STILL,goalState);


		while (true) {

			currentState = unchecked.pop(); // Take a state from the stack of states that should "checked"
			visited.push(currentState.getPoint()); // Declare that the state has been "visited"
			ArrayList<Point> listOfSurroundings = currentState.getSurroundings(); // Get all nearby points

			for (int i = 0; i < listOfSurroundings.size(); i++) { // Loop through all neighbours

				if (!(listOfObstacles.contains(listOfSurroundings.get(i)))) { // If a neighbour is not a obstacle...

					// Check if a state already has been created from the current point (We do not want duplicates!)
					boolean found = false;
					for (int j = 0; j < stateObjects.size(); j++) {
						if (stateObjects.get(j).getPoint().x == listOfSurroundings.get(i).x) {
							if (stateObjects.get(j).getPoint().y == listOfSurroundings.get(i).y) {
								availableState = stateObjects.get(j); // If a match is found, use the existing state object instead of creating a new one
								found = true;
								break; // Stop searching if a match is found since there shouldn't be any duplicates
							}
						}
					}

					if (!found) { // If no state object with the coordinates has been created, create a new object
						availableState = new States(listOfSurroundings.get(i));
						stateObjects.add(availableState); // Add the new state object to the list of created state objects

					}

					if (!(visited.contains(availableState.getPoint())) && !(outOfBounds(availableState, om))) { //Check if the available state already has been checked

						//Compare y-coordinates, if they are equal, the movement has to be in the x-direction
						if ((listOfSurroundings.get(i).getY() == currentState.getPoint().getY())) {
							if ((listOfSurroundings.get(i).getX()) < currentState.getPoint().getX()) {
								//WEST
								findTransition(goal, start, startState, goalState, model, currentState, availableState, movement.EAST);

							} else {
								//EAST
								findTransition(goal, start, startState, goalState, model, currentState, availableState, movement.WEST);
							}
						} else { //If y-coordinates is not equal, movement has to be in y-direction
							if ((listOfSurroundings.get(i).getY() < currentState.getPoint().getY())) {
								//NORTH
								findTransition(goal, start, startState, goalState, model, currentState, availableState, movement.SOUTH);
							} else {
								//SOUTH
								findTransition(goal, start, startState, goalState, model, currentState, availableState, movement.NORTH);
							}
						}
						unchecked.push(availableState);
					}
				}
			}

			if (unchecked.isEmpty()) {
				if(!startIsFound){
					model.addTransition(startState,movement.STILL, startState);
					model.setRewardFor(startState,-1);
				}
				break;
			}
		}
		return model;
	}


	private static void findTransition (Point goal, Point start, State startState, State goalState, ListbasedNongenericWorldModel model, States current, States available, movement direction) {

		movement oppositeDirection = null;

		// Find opposite direction
		switch (direction){
			case EAST -> oppositeDirection=movement.WEST;
			case NORTH -> oppositeDirection=movement.SOUTH;
			case WEST -> oppositeDirection=movement.EAST;
			case SOUTH -> oppositeDirection=movement.NORTH;
		}

		// Declare all outcomes including special cases

		// If current position is goal
		if ((current.getPoint().x == goal.x && current.getPoint().y == goal.y) && (available.getPoint().x != start.x)) {
			model.addTransition(available, direction, goalState);
			model.addTransition(goalState, oppositeDirection, available);
			model.setRewardFor(goalState, 0);
			model.setRewardFor(available,-1);

			// If current position is start
		} else if ((current.getPoint().y == start.y && current.getPoint().x == start.x) && (available.getPoint().x != goal.x)) {
			model.addTransition(available, direction, startState);
			model.addTransition(startState, oppositeDirection, available);
			model.setRewardFor(startState, -1);
			model.setRewardFor(available,-1);
			startIsFound=true;

			// If available position is goal
		} else if (available.getPoint().x == goal.x && available.getPoint().y == goal.y) {
			model.addTransition(goalState, direction, current);
			model.addTransition(current, oppositeDirection, goalState);
			model.setRewardFor(current, -1);
			model.setRewardFor(goalState,0);

			// If available position is start
		} else if ((available.getPoint().y == start.y && available.getPoint().x == start.x) && (current.getPoint().x != goal.x)) {
			model.addTransition(startState, direction, current);
			model.addTransition(current, oppositeDirection, startState);
			model.setRewardFor(current, -1);
			model.setRewardFor(startState,-1);
			startIsFound=true;


			// If current position is goal and available position is start
		} else if ((current.getPoint().x == goal.x && current.getPoint().y == goal.y) && available.getPoint().y == start.y && available.getPoint().x == start.x) {
			model.addTransition(startState, direction, goalState);
			model.addTransition(goalState, oppositeDirection, startState);
			model.setRewardFor(goalState, 0);
			model.setRewardFor(startState,-1);
			startIsFound=true;

			// If current position is start and available position is goal
		} else if ((current.getPoint().y == start.y && current.getPoint().x == start.x) && (available.getPoint().x == goal.x && available.getPoint().y == goal.y)) {
			model.addTransition(goalState, direction, startState);
			model.addTransition(startState, oppositeDirection, goalState);
			model.setRewardFor(startState, -1);
			model.setRewardFor(goalState,0);
			startIsFound=true;

			// If none of the above
		} else {
			model.addTransition(available, direction, current);
			model.addTransition(current, oppositeDirection, available);
			model.setRewardFor(current, -1);
			model.setRewardFor(available,-1);
		}
	}


	private static boolean outOfBounds (States available, ObstacleMap model) {
		if (available.getPoint().x<0 || available.getPoint().x>model.getWidth() || available.getPoint().y<0 || available.getPoint().y>model.getHeight() || available.getPoint()==null){
			return true;
		} else {
			return false;
		}
	}
}