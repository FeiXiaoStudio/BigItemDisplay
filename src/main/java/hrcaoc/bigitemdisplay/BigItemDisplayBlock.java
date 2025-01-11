package hrcaoc.bigitemdisplay;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;

public class BigItemDisplayBlock extends Block {
    public static final BooleanProperty WITH_MAP = BooleanProperty.of("map");
    public static final BooleanProperty GLOW = BooleanProperty.of("glow");

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WITH_MAP);
        builder.add(GLOW);
    }

    public BigItemDisplayBlock(Settings settings) {
        super(settings);

        // Set the default state of the block to false.
        setDefaultState(getDefaultState().with(WITH_MAP, false));
        setDefaultState(getDefaultState().with(GLOW, false));
    }

    public static int getLuminance(BlockState currentBlockState) {
        boolean glow = currentBlockState.get(BigItemDisplayBlock.GLOW);
        return glow ? 15 : 0;
    }

}
