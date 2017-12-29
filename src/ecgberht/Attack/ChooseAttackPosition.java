package ecgberht.Attack;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;
import ecgberht.GameState;

import bwapi.Pair;
import bwapi.TilePosition;

public class ChooseAttackPosition extends Conditional {

	public ChooseAttackPosition(String name, GameHandler gh) {
		super(name, gh);
	}

	@Override
	public State execute() {
		try {
			Pair<Integer,Integer> p = ((GameState)this.handler).inMap.getPosition(((GameState)this.handler).initAttackPosition,true);
			if(p.first != null && p.second != null) {
				((GameState)this.handler).attackPosition = new TilePosition(p.second,p.first).toPosition();
				return State.SUCCESS;
			}
			return State.FAILURE;
		} catch(Exception e) {
			System.err.println(this.getClass().getSimpleName());
			System.err.println(e);
			return State.ERROR;
		}
	}
}
