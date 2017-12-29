package ecgberht;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import org.iaie.btree.util.GameHandler;
import ecgberht.BuildingMap;

import bwapi.Color;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Pair;
import bwapi.Player;
import bwapi.Position;
import bwapi.TechType;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwapi.Utils;
import bwta.BWTA;
import bwta.BaseLocation;
import bwta.Chokepoint;
import bwta.Region;

public class GameState extends GameHandler {

	public int mining = 0;
	public Set<String> teamNames = new HashSet<String>(Arrays.asList("Alpha","Bravo","Charlie","Delta","Echo","Foxtrot","Golf","Hotel","India","Juliet","Kilo","Lima","Mike","November","Oscar","Papa","Quebec","Romeo","Sierra","Tango","Uniform","Victor","Whiskey","X-Ray","Yankee","Zulu"));
	public Map<String,Squad> squads = new HashMap<String,Squad>();
	public int trainedWorkers;
	public int builtBuildings;
	public int builtCC;
	public int builtRefinery;
	public int trainedCombatUnits;
	public boolean defense = false;
	public BaseLocation enemyBase = null;
	public Unit chosenWorker = null;
	public Unit chosenBunker = null;
	public Unit choosenScout = null;
	public Unit chosenBuilding = null;
	public UnitType chosenUnit = null;
	public UnitType chosenToBuild = null;
	public TilePosition chosenPosition = null;
	public String chosenSquad = null;
	public Pair<Integer,Integer> deltaCash = new Pair<Integer,Integer>(0,0);
	public int deltaSupply;
	public Unit MainCC = null;
	public Set<Unit> CCs = new HashSet<Unit>();
	public Set<Unit> CSs = new HashSet<Unit>();
	public Set<Unit> MBs = new HashSet<Unit>();
	public Set<Unit> UBs = new HashSet<Unit>();
	public Set<Unit> SBs = new HashSet<Unit>();
	public List<Pair<Unit,List<Unit>>> DBs = new ArrayList<Pair<Unit,List<Unit>>>();
	public List<Pair<Unit,Position> > workerDefenders = new ArrayList<Pair<Unit,Position> >();
	public List<Unit> workerIdle = new ArrayList<Unit>();
	public List<Pair<Unit,Unit> > workerTask = new ArrayList<Pair<Unit,Unit> >();
	public List<Pair<Unit,Unit> > repairerTask = new ArrayList<Pair<Unit,Unit> >();
	public List<Pair<Unit,Pair<UnitType,TilePosition>>> workerBuild = new ArrayList<Pair<Unit,Pair<UnitType,TilePosition>>>();
	public List<Pair<Unit,Integer> > mineralsAssigned = new ArrayList<Pair<Unit,Integer> >();
	public List<Pair<Pair<Unit,Integer>,Boolean> > refineriesAssigned = new ArrayList<Pair<Pair<Unit,Integer>,Boolean> >();
	public HashSet<Unit> enemyCombatUnitMemory = new HashSet<Unit>();
	public Set<Unit> enemyBuildingMemory = new HashSet<Unit>();
	public Set<BaseLocation> SLs = new HashSet<BaseLocation>();
	public List<Pair<Unit,Position> > Ms = new ArrayList<Pair<Unit,Position> >();
	public Set<Unit> Ts = new HashSet<Unit>();
	public Set<BaseLocation> BLs = new HashSet<BaseLocation>();
	public Set<BaseLocation> ScoutSLs = new HashSet<BaseLocation>();
	public Unit chosenUnitUpgrader = null;
	public UpgradeType chosenUpgrade = null;
	public Unit chosenRepairer = null;
	public Unit chosenBuildingRepair = null;
	public Unit chosenBuilderBL = null;
	public BuildingMap map;
	public BuildingMap testMap;
	public InfluenceMap inMap;
	public InfluenceMap inMapUnits;
	public Position attackPosition;
	public TilePosition chosenBaseLocation;
	public TilePosition initAttackPosition;
	public TilePosition initDefensePosition;
	public boolean expanding = false;
	public TilePosition closestChoke;
	public boolean movingToExpand = false;
	public TechType chosenResearch;
	public Unit chosenBuildingAddon;
	public UnitType chosenAddon;
	public List<Unit> buildingLot = new ArrayList<Unit>();
	public Unit chosenBuildingLot;
	public Pair<Unit, Position> chosenMarine;
	public List<Unit> enemyInBase = new ArrayList<Unit>();
	public boolean initCount = false;
	public boolean activeCount = false;
	public int startCount;
	public TilePosition checkScan;

	public GameState(Mirror bwapi) {
		super(bwapi);
		map = new BuildingMap(game,self);
		map.initMap();
		testMap = map.clone();
		inMap = new InfluenceMap(game,self,game.mapHeight(), game.mapWidth());

	}

	public void playSound(String soundFile) {
		try{
			File f = new File(soundFile);
			AudioInputStream audioIn = AudioSystem.getAudioInputStream(f.toURI().toURL());
			Clip clip = AudioSystem.getClip();
			clip.open(audioIn);
			clip.start();
		}
		catch(Exception e) {
			System.err.println(e);
		}
	}

	public Game getGame(){
		return game;
	}

	public Player getPlayer(){
		return self;
	}

	public void initCCs(){
		List<Unit> units = self.getUnits();
		for(Unit u:units) {
			if(u.getType() == UnitType.Terran_Command_Center) {
				CCs.add(u);
			}
		}
	}

	public void initWorkers(){
		List<Unit> units = self.getUnits();
		for(Unit u:units) {
			if(u.getType() == UnitType.Terran_SCV) {
				Pair<Unit,Unit> worker = new Pair<Unit,Unit>(u,null);
				workerTask.add(worker);
			}
		}
	}

	public void addNewResources(Unit unit) {
		List<Unit> minerals = BWTA.getNearestBaseLocation(unit.getTilePosition()).getMinerals();
		List<Unit> gas = BWTA.getNearestBaseLocation(unit.getTilePosition()).getGeysers();
		List<Pair<Unit,Integer> > auxMinerals = new ArrayList<Pair<Unit,Integer> >();
		List<Pair<Pair<Unit,Integer>,Boolean> > auxGas = new ArrayList<Pair<Pair<Unit,Integer>,Boolean> >();
		for(Unit m : minerals) {
			Pair<Unit,Integer> mineral = new Pair<Unit,Integer>(m,0);
			mineralsAssigned.add(mineral);
		}
		mineralsAssigned.addAll(auxMinerals);
		for(Unit m:gas) {
			Pair<Pair<Unit,Integer>,Boolean> geyser = new Pair<Pair<Unit,Integer>,Boolean>(new Pair<Unit,Integer>(m,0),false);
			refineriesAssigned.add(geyser);
		}
		refineriesAssigned.addAll(auxGas);

	}

	public void removeResources(Unit unit) {
		List<Unit> minerals = BWTA.getNearestBaseLocation(unit.getTilePosition()).getMinerals();
		List<Unit> gas = BWTA.getNearestBaseLocation(unit.getTilePosition()).getGeysers();
		List<Pair<Unit,Integer> > auxMinerals = new ArrayList<Pair<Unit,Integer> >();
		List<Pair<Pair<Unit,Integer>,Boolean> > auxGas = new ArrayList<Pair<Pair<Unit,Integer>,Boolean> >();
		for(Pair<Unit,Integer> pm : mineralsAssigned) {
			for(Unit m : minerals) {
				if(pm.first.equals(m)) {
					List<Pair<Unit,Unit> > aux = new ArrayList<Pair<Unit,Unit> >();
					for(Pair<Unit,Unit> w: workerTask) {
						if(pm.first.equals(w.second)) {
							aux.add(w);
							workerIdle.add(w.first);
						}
					}
					workerTask.removeAll(aux);
					auxMinerals.add(pm);
				}
			}
		}
		mineralsAssigned.removeAll(auxMinerals);
		for(Unit m : gas) {
			Pair<Pair<Unit,Integer>,Boolean> geyser = new Pair<Pair<Unit,Integer>,Boolean>(new Pair<Unit,Integer>(m,0),false);
			refineriesAssigned.add(geyser);
		}
		for(Pair<Pair<Unit,Integer>,Boolean> pm : refineriesAssigned) {
			for(Unit m : gas) {
				if(pm.first.first.equals(m)) {
					List<Pair<Unit,Unit> > aux = new ArrayList<Pair<Unit,Unit> >();
					for(Pair<Unit,Unit> w: workerTask) {
						if(pm.first.first.equals(w.second)) {
							aux.add(w);
							workerIdle.add(w.first);
						}
					}
					workerTask.removeAll(aux);
					auxGas.add(pm);
				}
			}
		}
		refineriesAssigned.removeAll(auxGas);
	}

	public Pair<Integer,Integer> getCash(){
		return new Pair<Integer,Integer>(self.minerals(),self.gas());
	}

	public int getSupply(){
		return (self.supplyTotal()-self.supplyUsed());
	}

	public void printer() {
		game.drawTextScreen(10, 50, Utils.formatText("APM: ",Utils.White) + Utils.formatText(String.valueOf(game.getAPM()), Utils.White));
		if(closestChoke != null) {
			game.drawTextMap(closestChoke.toPosition(), "Choke");
		}
		if(chosenBuilderBL != null) {
			game.drawTextMap(chosenBuilderBL.getPosition(), "BuilderBL");
			print(chosenBuilderBL,Color.Blue);
		}
		if (chosenBaseLocation != null) {
			print(chosenBaseLocation,UnitType.Terran_Command_Center,Color.Cyan);
		}
		for(Pair<Unit,Pair<UnitType,TilePosition> > u : workerBuild) {
			print(u.second.second,u.second.first,Color.Teal);
		}
		for(Pair<Unit,Unit> r : repairerTask) {
			print(r.first,Color.Yellow);
			game.drawTextMap(r.first.getPosition(), "Repairer");
		}
		game.drawTextScreen(10, 5, Utils.formatText(self.getName(), Utils.Green) + Utils.formatText(" vs ", Utils.Yellow) + Utils.formatText(game.enemy().getName(), Utils.Red));
		if (choosenScout != null) {
			game.drawTextMap(choosenScout.getPosition(), "Scouter");
			print(choosenScout,Color.Purple);
			game.drawTextScreen(10, 20, Utils.formatText("Scouting: ",Utils.White) + Utils.formatText("True", Utils.Green));
		}
		else {
			game.drawTextScreen(10, 20, Utils.formatText("Scouting: ",Utils.White) + Utils.formatText("False", Utils.Red));
		}
		if(enemyBase != null) {
			game.drawTextScreen(10, 35, Utils.formatText("Base enemiga encontrada: ",Utils.White) + Utils.formatText("True", Utils.Green));
		}
		else {
			game.drawTextScreen(10, 35, Utils.formatText("Base enemiga encontrada: ",Utils.White) + Utils.formatText("False", Utils.Red));
		}
		if (chosenWorker != null) {
			game.drawTextMap(chosenWorker.getPosition(), "ChosenWorker");
		}
		if (chosenRepairer != null) {
			game.drawTextMap(chosenRepairer.getPosition(), "ChosenRepairer");
		}
		if(enemyCombatUnitMemory.size()>0) {
			for(Unit u : enemyCombatUnitMemory) {
				game.drawTextMap(u.getPosition(), u.getType().toString());
				print(u,Color.Red);
			}
		}
		List <Region> regions = BWTA.getRegions();
		for(Region reg: regions) {
			List <Chokepoint> ch = reg.getChokepoints();
			for(Chokepoint c : ch) {
				Pair <Position,Position> lados = c.getSides();
				game.drawLineMap(lados.first, lados.second, Color.Green);
			}
		}
		for(Unit u: CCs) {
			print(u,Color.Yellow);
			game.drawCircleMap(u.getPosition(), 500, Color.Orange);
		}
		for(Pair<Unit,List<Unit> > u : DBs) {
			game.drawCircleMap(u.first.getPosition(), 300, Color.Orange);
		}
		for(Unit u: workerIdle) {
			print(u,Color.Green);
		}
		for(Pair<Unit, Position> u: workerDefenders) {
			print(u.first,Color.Purple);
			game.drawTextMap(u.first.getPosition(), "Spartan");
		}
		for(Entry<String, Squad> s : squads.entrySet()) {
			System.out.println(s);
			Position centro = getSquadCenter(s.getValue());
			game.drawCircleMap(centro, 90, Color.Green);
			game.drawTextMap(centro,s.getKey());
		}
	}

	public void print(Unit u,Color color) {
		game.drawBoxMap(u.getLeft(),u.getTop(),u.getRight(),u.getBottom(),color);
	}

	public void print(TilePosition u,UnitType type, Color color) {
		Position leftTop = new Position(u.getX() * TilePosition.SIZE_IN_PIXELS, u.getY() * TilePosition.SIZE_IN_PIXELS);
		Position rightBottom = new Position(leftTop.getX() + type.tileWidth() * TilePosition.SIZE_IN_PIXELS, leftTop.getY() + type.tileHeight() * TilePosition.SIZE_IN_PIXELS);
		game.drawBoxMap(leftTop,rightBottom,color);
	}

	public void print(TilePosition u, Color color) {
		Position leftTop = new Position(u.getX() * TilePosition.SIZE_IN_PIXELS, u.getY() * TilePosition.SIZE_IN_PIXELS);
		Position rightBottom = new Position(leftTop.getX() + TilePosition.SIZE_IN_PIXELS, leftTop.getY() + TilePosition.SIZE_IN_PIXELS);
		game.drawBoxMap(leftTop,rightBottom,color);
	}

	public void updateEnemyCombatUnits() {
		for (Unit u : game.enemy().getUnits()) {
			//Si no es un edificio ni un trabajador
			if (!u.getType().isBuilding() && !u.getType().isWorker()) {
				//Si no estaba en la lista le metemos
				if (!enemyCombatUnitMemory.contains(u)) enemyCombatUnitMemory.add(u);
			}
		}

		//Iteramos sobre las unidades de combate enemigas
		for (Unit p : enemyCombatUnitMemory) {
			// Sacamos su posicion
			TilePosition tileCorrespondingToP = new TilePosition(p.getPosition().getX()/32, p.getPosition().getY()/32);

			// Si la posicion es visible comprobamos si la unidad sigue alli
			if (game.isVisible(tileCorrespondingToP)) {
				boolean enemyCombatUnitVisible = false;
				for (Unit u : game.enemy().getUnits()) {
					if (!u.getType().isBuilding() && !u.getType().isWorker() && u.getPosition().equals(p.getPosition())) {
						enemyCombatUnitVisible = true;
						break;
					}
				}
				// La eliminamos si deja de ser visible
				if (enemyCombatUnitVisible == false) {
					enemyCombatUnitMemory.remove(p);
					break;
				}

			}
		}
	}

	public String convertSeconds(int seconds){
		int h = seconds/ 3600;
		int m = (seconds % 3600) / 60;
		int s = seconds % 60;
		String sh = (h > 0 ? String.valueOf(h) + " " + "h" : "");
		String sm = (m < 10 && m > 0 && h > 0 ? "0" : "") + (m > 0 ? (h > 0 && s == 0 ? String.valueOf(m) : String.valueOf(m) + " " + "min") : "");
		String ss = (s == 0 && (h > 0 || m > 0) ? "" : (s < 10 && (h > 0 || m > 0) ? "0" : "") + String.valueOf(s) + " " + "sec");
		return sh + (h > 0 ? " " : "") + sm + (m > 0 ? " " : "") + ss;
	}

	public void initStartLocations() {
		BaseLocation startBot = BWTA.getStartLocation(getPlayer());
		for (BaseLocation b : BWTA.getBaseLocations()) {
			if (b.isStartLocation() && !b.getTilePosition().equals(startBot.getTilePosition())) {
				SLs.add(b);
				ScoutSLs.add(b);
			}
		}
	}

	public void initBaseLocations() {
		for (BaseLocation b : BWTA.getBaseLocations()) {
			BLs.add(b);
		}
	}

	public void moveUnitFromChokeWhenExpand(){
		try {
			if(!Ms.isEmpty()) {
				List<Unit> radius = game.getUnitsInRadius(closestChoke.toPosition(), 500);
				if(!radius.isEmpty()) {
					List<Chokepoint> cs = BWTA.getRegion(chosenBaseLocation).getChokepoints();
					Chokepoint closestChoke = null;
					for(Chokepoint c : cs) {
						if(!c.getCenter().toTilePosition().equals(this.closestChoke)) {
							double aux = BWTA.getGroundDistance(c.getCenter().toTilePosition().makeValid(),chosenBaseLocation);
							if(aux > 0.0) {
								if(closestChoke == null ||  aux< BWTA.getGroundDistance(closestChoke.getCenter().toTilePosition().makeValid(),chosenBaseLocation)) {
									closestChoke = c;
								}
							}
						}
					}
					if(closestChoke != null) {
						for(Unit t : radius) {
							if(t.getPlayer().getID() == self.getID() && (t.getType() == UnitType.Terran_Marine || t.getType() == UnitType.Terran_Medic)) {
								t.attack(closestChoke.getCenter().makeValid());
							}
						}
					}
				}
			}
		} catch(Exception e) {
			System.err.println(e);
		}
	}

	public void fix() {
		if(chosenBuilderBL!= null && (chosenBuilderBL.isIdle() || chosenBuilderBL.isGatheringGas() || chosenBuilderBL.isGatheringMinerals())) {
			workerIdle.add(chosenBuilderBL);
			chosenBuilderBL = null;
			movingToExpand = false;
			expanding = false;
			chosenBaseLocation = null;
		}
		if(chosenBuilderBL!= null && workerIdle.contains(chosenBuilderBL)) {
			workerIdle.remove(chosenBuilderBL);
		}
		List<Pair<Unit,Unit> > aux = new ArrayList<Pair<Unit,Unit> >();
		List<Unit> aux2 = new ArrayList<Unit>();
		for(Pair<Unit,Unit> w : workerTask) {
			if(choosenScout != null && w.first.equals(choosenScout)) {
				choosenScout = null;
			}
			if(chosenRepairer != null && w.first.equals(chosenRepairer)) {
				chosenRepairer = null;
			}
			if(workerIdle.contains(w.first) && w.second.getType().isNeutral()) {
				if(w.first.isGatheringMinerals()) {
					aux2.add(w.first);
				}
				else if(w.first.isIdle()) {
					aux.add(w);
				}
			}
			if(w.first.isIdle() && w.second.getType().isNeutral()) {
				workerIdle.add(w.first);
				aux.add(w);
			}
		}
		workerTask.removeAll(aux);
		workerIdle.removeAll(aux2);
		List<Pair<Unit,Pair<UnitType,TilePosition>>> aux3 = new ArrayList<Pair<Unit,Pair<UnitType,TilePosition>>>();
		for(Pair<Unit,Pair<UnitType,TilePosition> > u : workerBuild) {
			if(choosenScout != null && u.first.equals(choosenScout)) {
				choosenScout = null;
			}
			if(chosenRepairer != null && u.first.equals(chosenRepairer)) {
				chosenRepairer = null;
			}
			if(u.first.isIdle() || u.first.isGatheringGas() || u.first.isGatheringMinerals()) {
				aux3.add(u);
				deltaCash.first -= u.second.first.mineralPrice();
				deltaCash.second -= u.second.first.gasPrice();
				workerIdle.add(u.first);
			}
		}
		workerBuild.removeAll(aux3);
		List<Pair<Unit,Unit> > aux4 = new ArrayList<Pair<Unit,Unit> >();
		for(Pair<Unit,Unit> r : repairerTask) {
			if(r.first.equals(choosenScout)) {
				choosenScout = null;
			}
			if(chosenRepairer != null) {
				if(!r.first.isRepairing() || r.first.isIdle()) {
					if(r.first.equals(chosenRepairer)) {
						chosenRepairer = null;
					}
					workerIdle.add(r.first);
					aux4.add(r);
				}
			}
		}
		repairerTask.removeAll(aux4);

		List<Pair<Unit,Position> > aux5 = new ArrayList<Pair<Unit,Position> >();
		for(Pair<Unit,Position> r : workerDefenders) {
			if(r.first.isIdle()) {
				workerIdle.add(r.first);
				aux5.add(r);
			}
		}
		workerDefenders.removeAll(aux5);
	}

	public void checkMainEnemyBase() {
		if(enemyBuildingMemory.isEmpty() && ScoutSLs.isEmpty()) {
			enemyBase = null;
			choosenScout = null;
			ScoutSLs.addAll(BLs);
			List<BaseLocation> aux = new ArrayList<BaseLocation>();
			for(BaseLocation b : ScoutSLs) {
				for(Unit u : CCs) {
					if(b.getTilePosition().equals(u.getTilePosition())) {
						if(!aux.contains(b)) {
							aux.add(b);
						}
						break;
					}
				}
				if(!BWTA.isConnected(self.getStartLocation(), b.getTilePosition())) {
					if(!aux.contains(b)) {
						aux.add(b);
					}
				}
				boolean found = false;
				if(game.isVisible(b.getTilePosition())) {
					for (Unit u : game.getUnitsInRadius(b.getPosition(), 500)) {
						if(u.getPlayer().getID() == game.enemy().getID() && u.getType().isBuilding()) {
							found = true;
						}
					}
					if(!found) {
						if(!aux.contains(b)) {
							aux.add(b);
						}
					}
				}
			}
			ScoutSLs.removeAll(aux);
		}
	}

	public void checkEnemyAttackingWT() {
		if(!workerTask.isEmpty()) {
			List<Pair<Unit,Unit> > aux = new ArrayList<Pair<Unit,Unit> >();
			for(Pair<Unit,Unit> p : workerTask) {
				if(p.second.getType().isBuilding() && !p.second.getType().isNeutral() && p.second.isBeingConstructed()) {
					if((p.first.isUnderAttack()) && (p.second.getType() != UnitType.Terran_Bunker && p.second.getType() != UnitType.Terran_Missile_Turret)) {
						p.first.haltConstruction();
						workerIdle.add(p.first);
						buildingLot.add(p.second);
						aux.add(p);
					}
				}
			}
			workerTask.removeAll(aux);
		}
	}

	public void initClosestChoke() {
		List<Chokepoint> cs = BWTA.getRegion(self.getStartLocation()).getChokepoints();
		BaseLocation closestBase = null;
		for(BaseLocation b : BLs) {
			if(BWTA.isConnected(self.getStartLocation(), b.getTilePosition())) {
				double bS = b.getGroundDistance(BWTA.getStartLocation(self));
				if(bS > 0.0) {
					if ((closestBase == null || bS < closestBase.getGroundDistance(BWTA.getStartLocation(self)))) {
						closestBase = b;
					}
				}
			}

		}
		if(closestBase != null) {
			Chokepoint closestChoke = null;
			for(Chokepoint c : cs) {
				double cS = BWTA.getGroundDistance(c.getCenter().toTilePosition(), closestBase.getTilePosition());
				if(cS > 0.0) {
					if ((closestChoke == null || cS < BWTA.getGroundDistance(closestChoke.getCenter().toTilePosition(), closestBase.getTilePosition()))) {
						closestChoke = c;
					}
				}

			}
			if(closestChoke != null) {
				this.closestChoke = closestChoke.getCenter().toTilePosition();
				initAttackPosition = this.closestChoke;
				initDefensePosition = this.closestChoke;
			}
		}
	}

	public void checkUnitsBL(TilePosition BL, Unit chosen) {
		UnitType type = UnitType.Terran_Command_Center;
		Position topLeft = new Position(BL.getX() * TilePosition.SIZE_IN_PIXELS, BL.getY() * TilePosition.SIZE_IN_PIXELS);
		Position bottomRight = new Position(topLeft.getX() + type.tileWidth() * TilePosition.SIZE_IN_PIXELS, topLeft.getY() + type.tileHeight() * TilePosition.SIZE_IN_PIXELS);
		List<Unit> blockers = game.getUnitsInRectangle(topLeft, bottomRight);
		if(!blockers.isEmpty()) {
			for(Unit u : blockers) {
				if(u.getPlayer().getID() == self.getID() && !u.equals(chosen) && !u.getType().isWorker()) {
					u.move(BWTA.getNearestChokepoint(BL).getCenter());
				}
			}
		}

	}

	public String getSquadName() {
		for(String nombre : teamNames) {
			if(!squads.containsKey(nombre)) {
				return nombre;
			}
		}
		return null;
	}

	public void addToSquad(Unit unit) {
		if(squads.size() == 0) {
			Squad aux = new Squad(getSquadName());
			aux.addToSquad(unit);
			squads.put(aux.name, aux);
		}
		else {
			String chosen = null;
			for(Entry<String, Squad> s : squads.entrySet()) {
				if(s.getValue().members.size() < 12) {
					chosen = s.getKey();
				}
			}
			if(chosen != null) {
				squads.get(chosen).addToSquad(unit);
			}
			else {
				Squad nuevo = new Squad(getSquadName());
				nuevo.addToSquad(unit);
				squads.put(nuevo.name, nuevo);
			}
		}
	}

	public Position getSquadCenter(Squad s) {
		Set<Unit> aux = s.members;
		Position point = new Position(0,0);
		for(Unit u : aux) {
			if(s.members.size() == 1) {
				return u.getPosition();
			}
			point = new Position(point.getX()+u.getPosition().getX(),point.getY()+u.getPosition().getY());

		}
		return new Position(point.getX()/aux.size(),point.getY()/aux.size());

	}

	public void removeFromSquad(Unit unit) {
		for(Entry<String, Squad> s : squads.entrySet()) {
			if(s.getValue().members.contains(unit)) {
				if(s.getValue().members.size() == 1) {
					squads.remove(s.getKey());
				}
				else {
					s.getValue().members.remove(unit);
				}
				break;
			}
		}
	}
	
}
