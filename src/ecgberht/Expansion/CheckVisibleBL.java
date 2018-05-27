package ecgberht.Expansion;

import ecgberht.GameState;
import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;
import org.openbw.bwapi4j.type.UnitType;
import org.openbw.bwapi4j.unit.Unit;

public class CheckVisibleBL extends Action {

    public CheckVisibleBL(String name, GameHandler gh) {
        super(name, gh);

    }

    @Override
    public State execute() {
        try {
            for (Unit u : ((GameState) this.handler).enemyCombatUnitMemory) {
                if (((GameState) this.handler).broodWarDistance(u.getPosition(), ((GameState) this.handler).chosenBaseLocation.toPosition()) < 300) {
                    ((GameState) this.handler).chosenBaseLocation = null;
                    ((GameState) this.handler).movingToExpand = false;
                    ((GameState) this.handler).chosenBuilderBL.stop(false);
                    ((GameState) this.handler).workerIdle.add(((GameState) this.handler).chosenBuilderBL);
                    ((GameState) this.handler).chosenBuilderBL = null;
                    ((GameState) this.handler).expanding = false;
                    ((GameState) this.handler).deltaCash.first -= UnitType.Terran_Command_Center.mineralPrice();
                    ((GameState) this.handler).deltaCash.second -= UnitType.Terran_Command_Center.gasPrice();
                    return State.FAILURE;
                }
            }
            if (((GameState) this.handler).getGame().getBWMap().isExplored(((GameState) this.handler).chosenBaseLocation)) {
                return State.SUCCESS;
            }
            return State.FAILURE;
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName());
            e.printStackTrace();
            return State.ERROR;
        }
    }
}
