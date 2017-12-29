package ecgberht.Repair;

import java.util.List;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;
import ecgberht.GameState;

import bwapi.Pair;
import bwapi.Unit;

public class CheckBuildingFlames extends Action {

	public CheckBuildingFlames(String name, GameHandler gh) {
		super(name, gh);
	}

	@Override
	public State execute() {
		try {
			boolean isBeingRepaired = false;
			for(Pair<Unit, List<Unit> > w : ((GameState)this.handler).DBs) {
				if(w.first.getType().maxHitPoints() != w.first.getHitPoints()) {
					for(Pair<Unit,Unit> r : ((GameState)this.handler).repairerTask) {
						if(w.first.equals(r.second)) {
							isBeingRepaired = true;
						}
					}
					if(!isBeingRepaired) {
						((GameState)this.handler).chosenBuildingRepair = w.first;
						return State.SUCCESS;
					}
				}
			}
			isBeingRepaired = false;
			for(Unit b : ((GameState)this.handler).Ts) {
				if(b.getType().maxHitPoints() != b.getHitPoints()) {
					for(Pair<Unit,Unit> r : ((GameState)this.handler).repairerTask) {
						if(b.equals(r.second)) {
							isBeingRepaired = true;
						}
					}
					if(!isBeingRepaired) {
						((GameState)this.handler).chosenBuildingRepair = b;
						return State.SUCCESS;
					}
				}
			}
			isBeingRepaired = false;
			for(Unit b : ((GameState)this.handler).MBs) {
				if(b.getType().maxHitPoints() != b.getHitPoints()) {
					for(Pair<Unit,Unit> r : ((GameState)this.handler).repairerTask) {
						if(b.equals(r.second)) {
							isBeingRepaired = true;
						}
					}
					if(!isBeingRepaired) {
						((GameState)this.handler).chosenBuildingRepair = b;
						return State.SUCCESS;
					}
				}
			}
			isBeingRepaired = false;
			for(Unit b : ((GameState)this.handler).UBs) {
				if(b.getType().maxHitPoints() != b.getHitPoints()) {
					for(Pair<Unit,Unit> r : ((GameState)this.handler).repairerTask) {
						if(b.equals(r.second)) {
							isBeingRepaired = true;
						}
					}
					if(!isBeingRepaired) {
						((GameState)this.handler).chosenBuildingRepair = b;
						return State.SUCCESS;
					}
				}
			}
			isBeingRepaired = false;
			for(Unit b : ((GameState)this.handler).SBs) {
				if(b.getType().maxHitPoints() != b.getHitPoints()) {
					for(Pair<Unit,Unit> r : ((GameState)this.handler).repairerTask) {
						if(b.equals(r.second)) {
							isBeingRepaired = true;
						}
					}
					if(!isBeingRepaired) {
						((GameState)this.handler).chosenBuildingRepair = b;
						return State.SUCCESS;
					}
				}
			}
			isBeingRepaired = false;
			for(Unit b : ((GameState)this.handler).CCs) {
				if(b.getType().maxHitPoints() != b.getHitPoints()) {
					for(Pair<Unit,Unit> r : ((GameState)this.handler).repairerTask) {
						if(b.equals(r.second)) {
							isBeingRepaired = true;
						}
					}
					if(!isBeingRepaired) {
						((GameState)this.handler).chosenBuildingRepair = b;
						return State.SUCCESS;
					}
				}
			}
			isBeingRepaired = false;
			for(Unit b : ((GameState)this.handler).CSs) {
				if(b.getType().maxHitPoints() != b.getHitPoints()) {
					for(Pair<Unit,Unit> r : ((GameState)this.handler).repairerTask) {
						if(b.equals(r.second)) {
							isBeingRepaired = true;
						}
					}
					if(!isBeingRepaired) {
						((GameState)this.handler).chosenBuildingRepair = b;
						return State.SUCCESS;
					}
				}
			}
			return State.FAILURE;
		} catch(Exception e) {
			System.err.println(this.getClass().getSimpleName());
			System.err.println(e);
			return State.ERROR;
		}
	}
}
