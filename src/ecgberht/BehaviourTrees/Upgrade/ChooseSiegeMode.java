package ecgberht.BehaviourTrees.Upgrade;

import ecgberht.GameState;
import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;
import org.openbw.bwapi4j.type.TechType;
import org.openbw.bwapi4j.unit.MachineShop;
import org.openbw.bwapi4j.unit.ResearchingFacility;

public class ChooseSiegeMode extends Action {

    public ChooseSiegeMode(String name, GameHandler gh) {
        super(name, gh);
    }

    @Override
    public State execute() {
        try {
            if (((GameState) this.handler).UBs.isEmpty()) {
                return State.FAILURE;
            }
            for (ResearchingFacility u : ((GameState) this.handler).UBs) {
                if (!(u instanceof MachineShop)) continue;
                if (!((GameState) this.handler).getPlayer().hasResearched(TechType.Tank_Siege_Mode) && u.canResearch(TechType.Tank_Siege_Mode) && !u.isResearching() && !u.isUpgrading()) {
                    ((GameState) this.handler).chosenUnitUpgrader = u;
                    ((GameState) this.handler).chosenResearch = TechType.Tank_Siege_Mode;
                    return State.SUCCESS;
                }
            }
            return State.FAILURE;
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName());
            e.printStackTrace();
            return State.ERROR;
        }
    }
}
