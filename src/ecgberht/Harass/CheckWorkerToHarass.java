package ecgberht.Harass;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

import bwapi.Unit;
import bwta.BWTA;
import ecgberht.GameState;

public class CheckWorkerToHarass extends Conditional {

	public CheckWorkerToHarass(String name, GameHandler gh) {
		super(name, gh);
	}

	@Override
	public State execute() {
		try {
			for(Unit u : ((GameState)this.handler).getGame().enemy().getUnits()) {
				if(((GameState)this.handler).enemyBase != null) {
					if(u.getType().isWorker() && !u.isGatheringGas()) {
						if(BWTA.getRegion(u.getPosition()).getCenter().equals(BWTA.getRegion(((GameState)this.handler).enemyBase.getPosition()).getCenter())){
							((GameState)this.handler).chosenUnitToHarass = u;
							return State.SUCCESS;
						}
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
