package ecgberht.Bother;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

import ecgberht.GameState;

public class CheckBotherer extends Conditional {

	public CheckBotherer(String name, GameHandler gh) {
		super(name, gh);
	}

	@Override
	public State execute() {
		try {
			if(((GameState)this.handler).chosenBotherer == null) {
				return State.FAILURE;
			}
			else{
				return State.SUCCESS;
			}
		} catch(Exception e) {
			System.err.println(this.getClass().getSimpleName());
			System.err.println(e);
			return State.ERROR;
		}
	}
}
