package ecgberht;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
//import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.iaie.btree.BehavioralTree;
import org.iaie.btree.task.composite.Selector;
import org.iaie.btree.task.composite.Sequence;
import org.iaie.btree.util.GameHandler;

import bwapi.*;
import bwta.BWTA;
import cameraModule.CameraModule;
import ecgberht.AddonBuild.*;
import ecgberht.Agents.Vulture;
import ecgberht.Attack.*;
import ecgberht.Build.*;
import ecgberht.BuildingLot.*;
import ecgberht.Bunker.*;
import ecgberht.CombatStim.*;
import ecgberht.Defense.*;
import ecgberht.Expansion.*;
import ecgberht.Harass.*;
import ecgberht.MoveToBuild.*;
import ecgberht.Recollection.*;
import ecgberht.Repair.*;
import ecgberht.Scanner.*;
import ecgberht.Scouting.*;
import ecgberht.Training.*;
import ecgberht.Upgrade.*;
//import ecgberht.Weka.Weka;
//import jweb.JBWEB;

public class Ecgberht extends DefaultBWListener {

	private Mirror mirror = new Mirror();
	private static Game game;
	private Player self;
	private static GameState gs;
	private BehavioralTree collectTree;
	private BehavioralTree trainTree;
	private BehavioralTree moveBuildTree;
	private BehavioralTree buildTree;
	private BehavioralTree scoutingTree;
	private BehavioralTree attackTree;
	private BehavioralTree defenseTree;
	private BehavioralTree upgradeTree;
	private BehavioralTree repairTree;
	private BehavioralTree expandTree;
	private BehavioralTree combatStimTree;
	private BehavioralTree addonBuildTree;
	private BehavioralTree buildingLotTree;
	private BehavioralTree bunkerTree;
	private BehavioralTree scannerTree;
	private BehavioralTree botherTree;
	private boolean first = false;
	private CameraModule observer;

	public void run() {
		mirror.getModule().setEventListener(this);
		mirror.startGame(false);
	}

	public static void main(String[] args) {
		new Ecgberht().run();
	}

	public static Game getGame() {
		return game;
	}

	public static GameState getGs() {
		return gs;
	}

	public void onStart() {
		
		//Disables System.err and System.Out
		OutputStream output = null;
		try {
			output = new FileOutputStream("NUL:");
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
		}
		PrintStream nullOut = new PrintStream(output);
		System.setErr(nullOut);
		System.setOut(nullOut);		

		game = mirror.getGame();
		self = game.self();
//		game.enableFlag(1);
//		game.setLocalSpeed(0);
		System.out.println("Analyzing map...");
//		BWTA.readMap();
		BWTA.analyze();
		System.out.println("Map data ready");
		observer = new CameraModule(self.getStartLocation().toPosition(), game);
		//observer.toggle();
		
		gs = new GameState(mirror);
		gs.initStartLocations();
		gs.initBaseLocations();
		gs.initBlockingMinerals();
		gs.checkBasesWithBLockingMinerals();
		gs.initClosestChoke();
		gs.initEnemyRace();
		gs.readOpponentInfo();
		if(gs.enemyRace == Race.Zerg) {
			if(gs.EI.naughty) {
				gs.playSound("rushed.mp3");
			}
		}
		gs.strat = gs.initStrat();

		//		try {
		//			System.out.println("Loading JBWEB...");
		//			long time = System.nanoTime();
		//			gs.jbweb.onStart();
		//			long end = System.nanoTime();
		//			System.out.println("Loaded");
		//			System.out.println("Time to load JBWEB(s): " + (end - time)*1e-9);
		//		} catch(Exception e) {
		//			System.err.println(e);
		//		}

		CollectGas cg = new CollectGas("Collect Gas", gs);
		CollectMineral cm = new CollectMineral("Collect Mineral", gs);
		FreeWorker fw = new FreeWorker("No Union", gs);
		Selector<GameHandler> collectResources = new Selector<GameHandler>("Collect Melted Cash",cg,cm);
		Sequence collect = new Sequence("Collect",fw,collectResources);
		collectTree = new BehavioralTree("Recollection Tree");
		collectTree.addChild(collect);

		ChooseSCV cSCV = new ChooseSCV("Choose SCV", gs);
		ChooseMarine cMar = new ChooseMarine("Choose Marine", gs);
		ChooseMedic cMed = new ChooseMedic("Choose Medic", gs);
		ChooseTank cTan = new ChooseTank("Choose Tank", gs);
		ChooseVulture cVul = new ChooseVulture("Choose vulture", gs);
		CheckResourcesUnit cr = new CheckResourcesUnit("Check Cash", gs);
		TrainUnit tr = new TrainUnit("Train SCV", gs);
		//Selector<GameHandler> chooseUnit = new Selector<GameHandler>("Choose Recruit",cSCV,cTan,cMed,cMar);
		Selector<GameHandler> chooseUnit = new Selector<GameHandler>("Choose Recruit",cSCV);
		
		if(gs.strat.trainUnits.contains(UnitType.Terran_Siege_Tank_Tank_Mode)) {
			chooseUnit.addChild(cTan);
		}
		if(gs.strat.trainUnits.contains(UnitType.Terran_Vulture)) {
			chooseUnit.addChild(cVul);
		}
		if(gs.strat.trainUnits.contains(UnitType.Terran_Medic)) {
			chooseUnit.addChild(cMed);
		}
		if(gs.strat.trainUnits.contains(UnitType.Terran_Marine)) {
			chooseUnit.addChild(cMar);
		}
		Sequence train = new Sequence("Train",chooseUnit,cr,tr);
		trainTree = new BehavioralTree("Training Tree");
		trainTree.addChild(train);

		ChooseSupply cSup = new ChooseSupply("Choose Supply Depot", gs);
		ChooseBunker cBun = new ChooseBunker("Choose Bunker", gs);
		ChooseBarracks cBar = new ChooseBarracks("Choose Barracks", gs);
		ChooseFactory cFar = new ChooseFactory("Choose Factory", gs);
		ChoosePort cPor = new ChoosePort("Choose Star Port", gs);
		ChooseScience cSci = new ChooseScience("Choose Science Facility", gs);
		ChooseRefinery cRef = new ChooseRefinery("Choose Refinery", gs);
		ChooseBay cBay = new ChooseBay("Choose Bay", gs);
		ChooseTurret cTur = new ChooseTurret("Choose Turret", gs);
		ChooseAcademy cAca = new ChooseAcademy("Choose Academy", gs);
		CheckResourcesBuilding crb = new CheckResourcesBuilding("Check Cash", gs);
		ChoosePosition cp = new ChoosePosition("Choose Position", gs);
		ChooseWorker cw = new ChooseWorker("Choose Worker", gs);
		Move m = new Move("Move to chosen building position", gs);
		//Selector<GameHandler> chooseBuildingBuild = new Selector<GameHandler>("Choose Building to build",cSup,cBun,cTur,cRef,cAca,cBay,cBar,cFar);
		Selector<GameHandler> chooseBuildingBuild = new Selector<GameHandler>("Choose Building to build",cSup);
		if(gs.strat.bunker) {
			chooseBuildingBuild.addChild(cBun);
		}
		chooseBuildingBuild.addChild(cTur);
		chooseBuildingBuild.addChild(cRef);
		if(gs.strat.buildUnits.contains(UnitType.Terran_Academy)) {
			chooseBuildingBuild.addChild(cAca);
		}
		if(gs.strat.buildUnits.contains(UnitType.Terran_Engineering_Bay)) {
			chooseBuildingBuild.addChild(cBay);
		}
		if(gs.strat.buildUnits.contains(UnitType.Terran_Factory)) {
			chooseBuildingBuild.addChild(cFar);
		}
		if(gs.strat.buildUnits.contains(UnitType.Terran_Starport)) {
			chooseBuildingBuild.addChild(cPor);
		}
		if(gs.strat.buildUnits.contains(UnitType.Terran_Science_Facility)) {
			chooseBuildingBuild.addChild(cSci);
		}
		chooseBuildingBuild.addChild(cBar);
		Sequence move = new Sequence("Move",chooseBuildingBuild,cp,cw,crb,m);
		moveBuildTree = new BehavioralTree("Building Tree");
		moveBuildTree.addChild(move);

		CheckWorkerBuild cWB = new CheckWorkerBuild("Check WorkerBuild", gs);
		Build b = new Build("Build", gs);
		Sequence build = new Sequence("Build",cWB,b);
		buildTree = new BehavioralTree("Build Tree");
		buildTree.addChild(build);

		CheckScout cSc = new CheckScout("Check Scout", gs);
		ChooseScout chSc = new ChooseScout("Choose Scouter",gs);
		SendScout sSc = new SendScout("Send Scout",gs);
		CheckVisibleBase cVB = new CheckVisibleBase("Check visible Base", gs);
		CheckEnemyBaseVisible cEBV = new CheckEnemyBaseVisible("Check Enemy Base Visible",gs);
		Sequence scoutFalse = new Sequence("Scout False",cSc,chSc,sSc);
		Selector<GameHandler> EnemyFound = new Selector<GameHandler>("Enemy found in base location",cEBV,sSc);
		Sequence scoutTrue = new Sequence("Scout True",cVB,EnemyFound);
		Selector<GameHandler> Scouting = new Selector<GameHandler>("Select Scouting Plan",scoutFalse,scoutTrue);
		scoutingTree = new BehavioralTree("Movement Tree");
		scoutingTree.addChild(Scouting);

		CheckArmy cA = new CheckArmy("Check Army",gs);
		ChooseAttackPosition cAP = new ChooseAttackPosition("Choose Attack Position",gs);
		Sequence Attack = new Sequence("Attack",cA,cAP);
		attackTree = new BehavioralTree("Attack Tree");
		attackTree.addChild(Attack);

		CheckPerimeter cP = new CheckPerimeter("Check Perimeter",gs);
		ChooseDefensePosition cDP = new ChooseDefensePosition("Choose Defence Position",gs);
		SendDefenders sD = new SendDefenders("Send Defenders", gs);
		Sequence Defense = new Sequence("Defence",cP,cDP,sD);
		defenseTree = new BehavioralTree("Defence Tree");
		defenseTree.addChild(Defense);

		CheckResourcesUpgrade cRU = new CheckResourcesUpgrade("Check Resources Upgrade", gs);
		ChooseArmorInfUp cAIU = new ChooseArmorInfUp("Choose Armor inf upgrade", gs);
		ChooseWeaponInfUp cWIU = new ChooseWeaponInfUp("Choose Weapon inf upgrade", gs);
		ChooseMarineRange cMR = new ChooseMarineRange("Choose Marine Range upgrade", gs);
		ChooseStimUpgrade cSU = new ChooseStimUpgrade("Choose Stimpack upgrade", gs);
		ChooseSiegeMode cSM = new ChooseSiegeMode("Choose Siege Mode", gs);
		ResearchUpgrade rU = new ResearchUpgrade("Research Upgrade", gs);
		//Selector<GameHandler> ChooseUP = new Selector<GameHandler>("Choose Upgrade", cAIU, cWIU, cSU, cMR, cSM);
		Selector<GameHandler> ChooseUP = new Selector<GameHandler>("Choose Upgrade");
		if(gs.strat.upgradesToResearch.contains(UpgradeType.Terran_Infantry_Weapons)) {
			ChooseUP.addChild(cWIU);
		}
		if(gs.strat.upgradesToResearch.contains(UpgradeType.Terran_Infantry_Armor)) {
			ChooseUP.addChild(cAIU);
		}
		if(gs.strat.techToResearch.contains(TechType.Stim_Packs)) {
			ChooseUP.addChild(cSU);
		}
		if(gs.strat.upgradesToResearch.contains(UpgradeType.U_238_Shells)) {
			ChooseUP.addChild(cMR);
		}
		if(gs.strat.techToResearch.contains(TechType.Tank_Siege_Mode)) {
			ChooseUP.addChild(cSM);
		}
		Sequence Upgrader = new Sequence("Upgrader",ChooseUP,cRU,rU);
		upgradeTree = new BehavioralTree("Technology");
		upgradeTree.addChild(Upgrader);

		CheckBuildingFlames cBF = new CheckBuildingFlames("Check building in flames",gs);
		ChooseRepairer cR = new ChooseRepairer("Choose Repairer",gs);
		Repair R = new Repair("Repair Building",gs);
		Sequence Repair = new Sequence("Repair",cBF,cR,R);
		repairTree = new BehavioralTree("RepairTree");
		repairTree.addChild(Repair);

		CheckExpansion cE = new CheckExpansion("Check Expansion",gs);
		CheckResourcesCC cRCC = new CheckResourcesCC("Check Resources CC",gs);
		ChooseBaseLocation cBL = new ChooseBaseLocation("Choose Base Location",gs);
		ChooseBuilderBL cBBL = new ChooseBuilderBL("Chose Builder Base Location",gs);
		SendBuilderBL sBBL = new SendBuilderBL("Send Builder To BL",gs);
		CheckVisibleBL cVBL = new CheckVisibleBL("Check Visible BL",gs);
		Expand E = new Expand("Expand",gs);
		Sequence Expander = new Sequence("Expander", cE, cRCC, cBL, cBBL, sBBL,cVBL, E);
		expandTree = new BehavioralTree("Expand Tree");
		expandTree.addChild(Expander);

		CheckStimResearched cSR = new CheckStimResearched("Check if Stim Packs researched",gs);
		Stim S = new Stim("Use Stim",gs);
		Sequence Stimmer = new Sequence("Stimmer", cSR, S);
		combatStimTree = new BehavioralTree("CombatStim Tree");
		combatStimTree.addChild(Stimmer);

		BuildAddon bA = new BuildAddon("Build Addon",gs);
		CheckResourcesAddon cRA = new CheckResourcesAddon("Check Resources Addon",gs);
		ChooseComsatStation cCS = new ChooseComsatStation("Choose Comsat Station",gs);
		ChooseMachineShop cMS = new ChooseMachineShop("Choose Machine Shop",gs);
		Selector<GameHandler> ChooseAddon = new Selector<GameHandler>("Choose Addon");
		if(gs.strat.buildAddons.contains(UnitType.Terran_Machine_Shop)) {
			ChooseAddon.addChild(cMS);
		}
		if(gs.strat.buildAddons.contains(UnitType.Terran_Comsat_Station)) {
			ChooseAddon.addChild(cCS);
		}
		Sequence Addon = new Sequence("Addon", ChooseAddon, cRA, bA);
		addonBuildTree = new BehavioralTree("Addon Build Tree");
		addonBuildTree.addChild(Addon);

		CheckBuildingsLot chBL = new CheckBuildingsLot("Check Buildings Lot", gs);
		ChooseBlotWorker cBW = new ChooseBlotWorker("Choose Building Lot worker", gs);
		ChooseBuildingLot cBLot = new ChooseBuildingLot("Choose Building Lot building", gs);
		FinishBuilding fB = new FinishBuilding("Finish Building", gs);
		Sequence BLot = new Sequence("Building Lot", chBL, cBLot, cBW, fB);
		buildingLotTree = new BehavioralTree("Building Lot Tree");
		buildingLotTree.addChild(BLot);

		ChooseBunkerToLoad cBu = new ChooseBunkerToLoad("Choose Bunker to Load",gs);
		EnterBunker eB = new EnterBunker("Enter bunker",gs);
		ChooseMarineToEnter cMTE = new ChooseMarineToEnter("Choose Marine To Enter", gs);
		Sequence Bunker = new Sequence("Bunker", cBu, cMTE, eB);
		bunkerTree = new BehavioralTree("Bunker Tree");
		bunkerTree.addChild(Bunker);

		CheckScan cScan = new CheckScan("Check scan",gs);
		Scan s = new Scan("Scan",gs);
		Sequence Scanning = new Sequence("Scanning", cScan, s);
		scannerTree = new BehavioralTree("Scanner Tree");
		scannerTree.addChild(Scanning);

		CheckHarasser cH = new CheckHarasser("Check Harasser", gs);
		ChooseWorkerToHarass cWTH = new ChooseWorkerToHarass("Check Worker to Harass", gs);
		ChooseBuilderToHarass cWTB = new ChooseBuilderToHarass("Check Worker to Harass", gs);
		CheckHarasserAttacked cHA = new CheckHarasserAttacked("Check Harasser Attacked",gs);
		ChooseBuildingToHarass cBTH = new ChooseBuildingToHarass("Check Building to Harass", gs);
		HarassWorker hW = new HarassWorker("Bother SCV", gs);
		Selector<GameHandler> bOw = new Selector<GameHandler>("Choose Builder or Worker or Building",cWTH,cWTB, cBTH);
		Sequence harass = new Sequence("Harass", cH, cHA, bOw, hW);
		botherTree = new BehavioralTree("Harass Tree");
		botherTree.addChild(harass);
	}

	public void onFrame() {
		gs.frameCount = game.getFrameCount();
		gs.print(gs.naturalRegion.getCenter().toTilePosition(), Color.Red);
		observer.onFrame();
		gs.inMapUnits = new InfluenceMap(game,self,game.mapHeight(), game.mapWidth());
		gs.updateEnemyBuildingsMemory();
		gs.runAgents();
		//gs.checkEnemyAttackingWT();
		buildingLotTree.run();
		repairTree.run();
		collectTree.run();
		expandTree.run();
		upgradeTree.run();
		moveBuildTree.run();
		buildTree.run();
		addonBuildTree.run();
		trainTree.run();
		scoutingTree.run();
		botherTree.run();
		bunkerTree.run();
		scannerTree.run();
		if(gs.strat.name == "ProxyBBS") {
			gs.checkWorkerMilitia();
		}
		gs.siegeTanks();
		defenseTree.run();
		attackTree.run();
		gs.updateSquadOrderAndMicro();
		combatStimTree.run();
		gs.checkMainEnemyBase();
		gs.fix();
		gs.mergeSquads();
		if(game.elapsedTime() < 150 && gs.enemyBase != null && gs.enemyRace == Race.Zerg && !gs.EI.naughty) {
			boolean found_pool = false;
			int drones = game.enemy().allUnitCount(UnitType.Zerg_Drone);
			for(EnemyBuilding u  : gs.enemyBuildingMemory.values()) {
				if(u.type == UnitType.Zerg_Spawning_Pool) {
					found_pool = true;
					break;
				}
			}
			if(found_pool && drones <= 5) {
				gs.EI.naughty = true;
				game.sendText("Bad zerg!, bad!");
				gs.playSound("rushed.mp3");
			}
		}
		if(gs.frameCount > 0 && gs.frameCount % 5 == 0) {
			gs.mineralLocking();
		}
		gs.printer();
		//		try {
		//			gs.jbweb.draw();
		//		} catch(Exception e) {
		//			System.err.println();
		//		}

	}

	public void onEnd(boolean arg0) {
		String name = game.enemy().getName();
		gs.EI.updateStrategyOpponentHistory(gs.strat.name, gs.mapSize, arg0);
		if(arg0) {
			gs.EI.wins++;
			game.sendText("gg wp "+ name);
		} else {
			gs.EI.losses++;
			game.sendText("gg wp! "+ name + ", next game I will win!");
		}
		//		Weka weka = new Weka();
		//		try {
		//			weka.createAndWriteInstance(game.enemy().getName(),gs.strat.name, gs.mapSize, arg0);
		//		} catch (IOException e) {
		//			// TODO Auto-generated catch block
		//			System.err.println(e);
		//		}
		gs.writeOpponentInfo(name);
	}

	public void onNukeDetect(Position arg0) {

	}

	public void onPlayerDropped(Player arg0) {

	}

	public void onPlayerLeft(Player arg0) {

	}

	public void onReceiveText(Player arg0, String arg1) {

	}

	public void onSaveGame(String arg0) {

	}

	public void onSendText(String arg0) {

	}

	public void onUnitCreate(Unit arg0) {
		if(!arg0.getType().isNeutral() && !arg0.getType().isSpecialBuilding()) {
			if(arg0.getType().isBuilding()) {
				gs.inMap.updateMap(arg0,false);
				if(arg0.getPlayer().getID() == self.getID()) {
					if(arg0.getType() != UnitType.Terran_Command_Center) {
						gs.map.updateMap(arg0.getTilePosition(),arg0.getType(),false);
						gs.testMap = gs.map.clone();
					}
					for(Pair<Unit,Pair<UnitType,TilePosition> > u: gs.workerBuild) {
						if(u.first.equals(arg0.getBuildUnit()) && u.second.first.equals(arg0.getType())) {
							gs.workerTask.add(new Pair<Unit,Unit>(u.first,arg0));
							gs.deltaCash.first -= arg0.getType().mineralPrice();
							gs.deltaCash.second -= arg0.getType().gasPrice();
							gs.workerBuild.remove(u);
							break;
						}
					}
				}
			}
			else if(arg0.getType() == UnitType.Terran_Vulture && arg0.getPlayer().getID() == self.getID()) {
				gs.vulturesTrained++;
			}
		}
	}

	public void onUnitComplete(Unit arg0) {
		try {
			observer.moveCameraUnitCreated(arg0);
			UnitType type = arg0.getType();
			if(!type.isNeutral() && arg0.getPlayer().getID() == self.getID()) {
				if(type.isBuilding()) {
					gs.builtBuildings++;
					if(type.isRefinery()) {
						for(Pair<Pair<Unit,Integer>,Boolean> r:gs.refineriesAssigned) {
							if(r.first.first.getTilePosition().equals(arg0.getTilePosition())) {
								gs.refineriesAssigned.get(gs.refineriesAssigned.indexOf(r)).second = true;
								gs.refineriesAssigned.get(gs.refineriesAssigned.indexOf(r)).first.second++;
								break;
							}
						}
						gs.builtRefinery++;
					} else {
						if(type == UnitType.Terran_Command_Center) {
							gs.CCs.put(BWTA.getRegion(arg0.getPosition()).getCenter(),arg0);
							gs.addNewResources(arg0);
							if(arg0.getAddon() != null && !gs.CSs.contains(arg0.getAddon())) {
								gs.CSs.add(arg0.getAddon());
							}
							if(gs.frameCount == 0) {
								gs.MainCC = arg0;
							}
							gs.builtCC++;
						}
						if(type == UnitType.Terran_Comsat_Station) {
							gs.CSs.add(arg0);
						}
						if(type == UnitType.Terran_Bunker) {
							gs.DBs.put(arg0, new HashSet<Unit>());
						}
						if(type == UnitType.Terran_Engineering_Bay || type == UnitType.Terran_Academy) {
							gs.UBs.add(arg0);
						}
						if(type == UnitType.Terran_Barracks) {
							gs.MBs.add(arg0);
						}
						if(type == UnitType.Terran_Factory) {
							gs.Fs.add(arg0);
						}
						if(type == UnitType.Terran_Starport) {
							gs.Ps.add(arg0);
						}
						if(type == UnitType.Terran_Science_Facility) {
							gs.UBs.add(arg0);
						}
						if(type == UnitType.Terran_Supply_Depot) {
							gs.SBs.add(arg0);
						}
						if(type == UnitType.Terran_Machine_Shop) {
							gs.UBs.add(arg0);
						}
						if(type == UnitType.Terran_Missile_Turret) {
							gs.Ts.add(arg0);
						}
						for(Pair<Unit, Unit> u : gs.workerTask) {
							if(u.second.equals(arg0)) {
								gs.workerTask.remove(u);
								gs.workerIdle.add(u.first);
								break;
							}
						}
					}
				}
				else{
					if(type.isWorker()) {
						gs.workerIdle.add(arg0);
						gs.trainedWorkers++;
					}
					else{
						if(type == UnitType.Terran_Siege_Tank_Tank_Mode) {
							if(!gs.TTMs.containsKey(arg0)) {
								String nombre = gs.addToSquad(arg0);
								gs.TTMs.put(arg0,nombre);
								if(!gs.DBs.isEmpty()) {
									arg0.attack(gs.DBs.keySet().iterator().next().getPosition());
								}
								else if(gs.closestChoke != null) {
									arg0.attack(gs.closestChoke.getCenter());
								}else{
									arg0.attack(BWTA.getNearestChokepoint(self.getStartLocation()).getCenter());
								}
							}
							else {
								Squad tankS = gs.squads.get(gs.TTMs.get(arg0));
								Position beforeSiege = null;
								if(tankS != null) {
									 beforeSiege = tankS.attack;
								}
								if(beforeSiege != null && beforeSiege != Position.None) {
									arg0.attack(beforeSiege);
								}
							}
						}
						else if(type == UnitType.Terran_Vulture) {
							gs.agents.add(new Vulture(arg0));
						}
						else if(type == UnitType.Terran_Marine || type == UnitType.Terran_Medic) {
							gs.addToSquad(arg0);
							if(gs.strat.name != "ProxyBBS") {
								if(!gs.EI.naughty || gs.enemyRace != Race.Zerg) {
									if(!gs.DBs.isEmpty()) {
										arg0.attack(gs.DBs.keySet().iterator().next().getPosition());
									}
									else if(gs.closestChoke != null) {
										arg0.attack(gs.closestChoke.getCenter());
									}else{
										arg0.attack(BWTA.getNearestChokepoint(self.getStartLocation()).getCenter());
									}
								}
							}
							else {
								if(new TilePosition(game.mapWidth()/2, game.mapHeight()/2).getDistance(gs.enemyBase.getTilePosition()) < arg0.getTilePosition().getDistance(gs.enemyBase.getTilePosition())) {
									arg0.attack(new TilePosition(game.mapWidth()/2, game.mapHeight()/2).toPosition());
								}
							}
						}
						gs.trainedCombatUnits++;
					}
				}
			}
		} catch(Exception e) {
			System.err.println("onUnitComplete exception");
			System.err.println(e);
		}
		
	}

	public void onUnitDestroy(Unit arg0) {
		try {
			UnitType type = arg0.getType();
			if(type.isMineralField()) {
				if(gs.mineralsAssigned.containsKey(arg0)) {
					gs.map.updateMap(arg0.getTilePosition(), type, true);
					gs.testMap = gs.map.clone();
					List<Unit> aux = new ArrayList<>();
					for(Entry<Unit, Unit> w: gs.workerMining.entrySet()) {
						if(arg0.equals(w.getValue())) {
							w.getKey().stop();
							gs.workerIdle.add(w.getKey());
							aux.add(w.getKey());
						}
					}
					for(Unit u : aux) {
						gs.workerMining.remove(u);
					}
					gs.mineralsAssigned.remove(arg0);
				}
			}
			if(!type.isBuilding() && !type.isRefinery() && type != UnitType.Resource_Vespene_Geyser && type != UnitType.Spell_Scanner_Sweep) {
				if(!first ) {
					gs.playSound("first.mp3");
					first = true;
				}
			}
			if(!type.isNeutral()  && (!type.isSpecialBuilding() || type.isRefinery())) {
				if(arg0.getPlayer().getID() == game.enemy().getID()) {
					if(arg0.equals(gs.chosenUnitToHarass)) {
						gs.chosenUnitToHarass = null;
					}
					if(type.isBuilding()) {
						gs.inMap.updateMap(arg0,true);
						gs.enemyBuildingMemory.remove(arg0);
						gs.initAttackPosition = arg0.getTilePosition();
						gs.map.updateMap(arg0.getTilePosition(), type, true);
					} else {
						gs.initDefensePosition = arg0.getTilePosition();
					}
				}
				else if(arg0.getPlayer().getID() == self.getID()) {
					if(type.isWorker()) {
						if(gs.strat.name == "ProxyBBS") {
							gs.removeFromSquad(arg0);
						}
						for(Pair<Unit,Unit> r : gs.repairerTask) {
							if(r.first.equals(arg0)) {
								gs.repairerTask.remove(r);
								break;
							}
						}
						if(gs.workerIdle.contains(arg0)) {
							gs.workerIdle.remove(arg0);
						}
						if(gs.chosenScout != null && arg0.equals(gs.chosenScout)) {
							gs.chosenScout = null;
						}
						if(gs.chosenHarasser != null && arg0.equals(gs.chosenHarasser)) {
							gs.chosenHarasser = null;
							gs.chosenUnitToHarass = null;
						}
						if(gs.chosenWorker != null && arg0.equals(gs.chosenWorker)) {
							gs.chosenWorker = null;
						}
						for(Pair<Unit,Unit> r : gs.repairerTask) {
							if(arg0.equals(r.first)) {
								gs.repairerTask.remove(r);
								break;
							}
						}
						if(gs.chosenBuilderBL != null && arg0.equals(gs.chosenBuilderBL)) {
							gs.chosenBuilderBL = null;
							gs.expanding = false;
							gs.chosenBaseLocation = null;
							gs.movingToExpand = false;
							gs.deltaCash.first -= UnitType.Terran_Command_Center.mineralPrice();
							gs.deltaCash.second -= UnitType.Terran_Command_Center.gasPrice();
						}
						if(gs.workerDefenders.contains(arg0)) {
							gs.workerDefenders.remove(arg0);
						}
						
						if(gs.workerMining.containsKey(arg0)) {
							Unit mineral = gs.workerMining.get(arg0);
							gs.workerMining.remove(arg0);
							if(gs.mineralsAssigned.containsKey(mineral)) {
								gs.mining--;
								gs.mineralsAssigned.put(mineral, gs.mineralsAssigned.get(mineral) - 1);
							}
							
						}
						for(Pair<Unit,Unit> w: gs.workerTask) {
							if(w.first.equals(arg0)) {
								if(w.second.getType().isRefinery()) {
									for(Pair<Pair<Unit,Integer>,Boolean> r: gs.refineriesAssigned) {
										if(r.first.first.equals(w.second)) {
											gs.refineriesAssigned.get(gs.refineriesAssigned.indexOf(r)).first.second--;
											break;
										}
									}
								}
								else {
									if(w.second.getType().isBuilding() && !w.second.isCompleted()) {
										gs.buildingLot.add(w.second);
									}
								}
								gs.workerTask.remove(w);
								break;
							}
						}
						for(Pair<Unit,Pair<UnitType,TilePosition> > w: gs.workerBuild) {
							if(w.first.equals(arg0)) {
								gs.workerBuild.remove(w);
								gs.deltaCash.first -= w.second.first.mineralPrice();
								gs.deltaCash.second -= w.second.first.gasPrice();
								break;
							}
						}
					}	
					 else if(type.isBuilding()) {
		
						gs.inMap.updateMap(arg0,true);
						if(type != UnitType.Terran_Command_Center) {
							gs.map.updateMap(arg0.getTilePosition(), type, true);
						}
						for(Pair<Unit,Unit> r : gs.repairerTask) {
							if(r.second.equals(arg0)) {
								gs.workerIdle.add(r.first);
								gs.repairerTask.remove(r);
								break;
							}
						}
						for(Pair<Unit, Unit> w: gs.workerTask) {
							if(w.second.equals(arg0)) {
								gs.workerTask.remove(w);
								gs.workerIdle.add(w.first);
								break;
							}
						}
						for(Unit w: gs.buildingLot) {
							if(w.equals(arg0)) {
								gs.buildingLot.remove(w);
								break;
							}
						}
						for(Unit u : gs.CCs.values()) {
							if(u.equals(arg0)) {
								gs.removeResources(arg0);
								if(arg0.getAddon() != null && gs.CSs.contains(arg0.getAddon())) {
									gs.CSs.remove(arg0.getAddon());
								}
								gs.CCs.remove(BWTA.getRegion(arg0.getPosition()).getCenter());
								if(arg0.equals(gs.MainCC)) {
									if(gs.CCs.size() > 0) {
										for(Unit c : gs.CCs.values()) {
											if(!c.equals(arg0)) {
												gs.MainCC = u;
												break;
											}
										}
									}
									else {
										gs.MainCC = null;
										break;
									}
								}
							}
						}
		
						if(gs.CSs.contains(arg0)) {
							gs.CSs.remove(arg0);
						}
						if(gs.Fs.contains(arg0)) {
							gs.Fs.remove(arg0);
						}
						if(gs.MBs.contains(arg0)) {
							gs.MBs.remove(arg0);
						}
						if(gs.UBs.contains(arg0)) {
							gs.UBs.remove(arg0);
						}
						if(gs.SBs.contains(arg0)) {
							gs.SBs.remove(arg0);
						}
						if(gs.Ts.contains(arg0)) {
							gs.Ts.remove(arg0);
						}
						if(gs.Ps.contains(arg0)) {
							gs.Ps.remove(arg0);
						}	
						if(type == UnitType.Terran_Bunker) {
							if(gs.DBs.containsKey(arg0)) {
								for(Unit u : gs.DBs.get(arg0)) {
									gs.addToSquad(u);
								}
								gs.DBs.remove(arg0);
							}
						}
						if(type.isRefinery()) {
							for(Pair<Pair<Unit,Integer>,Boolean> r: gs.refineriesAssigned) {
								if(r.first.first.equals(arg0)) {
									gs.refineriesAssigned.get(gs.refineriesAssigned.indexOf(r)).second = false;
									List<Pair<Unit,Unit> > aux = new ArrayList<Pair<Unit,Unit> >();
									for(Pair<Unit,Unit> w: gs.workerTask) {
										if(r.first.first.equals(w.second)) {
											aux.add(w);
											gs.workerIdle.add(w.first);
										}
									}
									gs.workerTask.removeAll(aux);
									break;
								}
							}
						}
						gs.testMap = gs.map.clone();
					} else {
						if(type == UnitType.Terran_Siege_Tank_Siege_Mode || type == UnitType.Terran_Siege_Tank_Tank_Mode) {
							if(gs.TTMs.containsKey(arg0)) {
								gs.TTMs.remove(arg0);
								gs.removeFromSquad(arg0);
							}
						}
						else if(type == UnitType.Terran_Marine || type == UnitType.Terran_Medic) {
							gs.removeFromSquad(arg0);
						}
						else if(type == UnitType.Terran_Vulture) {
							gs.agents.remove(new Vulture(arg0));
						}
					}
				} 
			}
		} catch(Exception e) {
			System.err.println("OnUnitDestroy Exception");
			System.err.println(e);
		}
		
	}

	public void onUnitMorph(Unit arg0) {
		if(arg0.getPlayer().getID() == game.enemy().getID()) {
			if(arg0.getType().isBuilding() && !arg0.getType().isRefinery()) {
				if(!gs.enemyBuildingMemory.containsKey(arg0)) {
					gs.inMap.updateMap(arg0,false);
					gs.enemyBuildingMemory.put(arg0,new EnemyBuilding(arg0));
				}
			}
		}
		if(arg0.getType().isRefinery() && arg0.getPlayer().getID() == self.getID()) {
			for(Pair<Pair<Unit,Integer>,Boolean> r:gs.refineriesAssigned) {
				if(r.first.first.getTilePosition().equals(arg0.getTilePosition())) {
					gs.map.updateMap(arg0.getTilePosition(), arg0.getType(),false);
					gs.testMap = gs.map.clone();
					break;
				}
			}
			for(Pair<Unit,Pair<UnitType,TilePosition> > u: (gs.workerBuild)) {
				if(u.first.equals(arg0.getBuildUnit()) && u.second.first.equals(arg0.getType())) {
					gs.workerBuild.remove(u);
					gs.workerTask.add(new Pair<Unit,Unit>(u.first,arg0));
					gs.deltaCash.first -= arg0.getType().mineralPrice();
					gs.deltaCash.second -= arg0.getType().gasPrice();
					break;
				}
			}
		}
	}

	public void onUnitDiscover(Unit arg0) {

	}

	public void onUnitEvade(Unit arg0) {

	}

	public void onUnitHide(Unit arg0) {
		if(gs.enemyCombatUnitMemory.contains(arg0)) {
			gs.enemyCombatUnitMemory.remove(arg0);
		}
	}

	public void onUnitRenegade(Unit arg0) {

	}

	public void onUnitShow(Unit arg0) {
		if(game.enemy().getID() == arg0.getPlayer().getID()) {
			if(gs.enemyRace == Race.Unknown) {
				gs.enemyRace = arg0.getType().getRace();
			}
			if(!arg0.getType().isBuilding() || arg0.getType().canAttack()) {
				gs.enemyCombatUnitMemory.add(arg0);
			}
			if(arg0.getType().isBuilding()) {
				if(!gs.enemyBuildingMemory.containsKey(arg0)) {
					gs.enemyBuildingMemory.put(arg0,new EnemyBuilding(arg0));
					gs.inMap.updateMap(arg0,false);
					gs.map.updateMap(arg0.getTilePosition(), arg0.getType(), false);
				}
			}

		}
	}
}
