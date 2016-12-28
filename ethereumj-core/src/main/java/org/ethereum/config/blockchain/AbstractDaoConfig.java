package org.ethereum.config.blockchain;

import org.apache.commons.lang3.tuple.Pair;
import org.ethereum.config.BlockchainConfig;
import org.ethereum.core.BlockHeader;
import org.ethereum.validator.BlockHeaderRule;
import org.ethereum.validator.BlockHeaderValidator;
import org.ethereum.validator.ExtraDataPresenceRule;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Stan Reshetnyk on 26.12.16.
 */
public abstract class AbstractDaoConfig extends FrontierConfig {

    /**
     * Hardcoded values from live network
     */
    public static final long ETH_FORK_BLOCK_NUMBER = 1_920_000;

    public static final byte[] DAO_EXTRA_DATA = Hex.decode("64616f2d686172642d666f726b");

    // TODO find a way to remove block number value from blockchain config
    protected long forkBlockNumber;

    // set in child classes
    protected boolean supportFork;

    private List<Pair<Long, BlockHeaderValidator>> validator;

    private BlockchainConfig parent;

    protected void initDaoConfig(BlockchainConfig parent, long forkBlockNumber) {
        this.parent = parent;
        this.forkBlockNumber = forkBlockNumber;
        BlockHeaderRule rule = new ExtraDataPresenceRule(DAO_EXTRA_DATA, supportFork);
        validator = Arrays.asList(Pair.of(forkBlockNumber, new BlockHeaderValidator(Arrays.asList(rule))));
    }

    @Override
    public List<Pair<Long, BlockHeaderValidator>> headerValidators() {
        return validator;
    }

    @Override
    public BigInteger calcDifficulty(BlockHeader curBlock, BlockHeader parent) {
        return this.parent.calcDifficulty(curBlock, parent);
    }
}
