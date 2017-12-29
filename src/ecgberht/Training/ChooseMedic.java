package ecgberht.Training;

import java.util.List;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;
import ecgberht.GameState;

import bwapi.Pair;
import bwapi.Unit;
import bwapi.UnitType;


public class ChooseMedic extends Action {

	public ChooseMedic(String name, GameHandler gh) {
		super(name, gh);
	}

	@Override
	public State execute() {
		try {
			if(((GameState)this.handler).UBs.isEmpty()) {
				return State.FAILURE;
			}
			else {
				for(Unit u : ((GameState)this.handler).UBs) {
					if (u.getType() == UnitType.Terran_Academy) {
						int marine_count = 0;
						if(!((GameState)this.handler).DBs.isEmpty()) {
							for(Pair<Unit,List<Unit> > p : ((GameState)this.handler).DBs) {
								marine_count += p.second.size();
							}
						}
						if(!((GameState)this.handler).MBs.isEmpty() && ((GameState)this.handler).getPlayer().allUnitCount(UnitType.Terran_Medic) * 4 < ((GameState)this.handler).getPlayer().allUnitCount(UnitType.Terran_Marine) - marine_count) {
							for(Unit b:((GameState)this.handler).MBs) {
								if(!b.isTraining()) {
									((GameState)this.handler).chosenUnit = UnitType.Terran_Medic;
									((GameState)this.handler).chosenBuilding = b;
									return State.SUCCESS;
								}
							}
						}
						break;
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
