package org.ethereum.net.eth.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.ethereum.db.BlockStore;
import org.ethereum.listener.EthereumListener;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.*;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.net.MessageQueue;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.net.eth.message.*;
import org.ethereum.net.message.ReasonCode;
import org.ethereum.net.server.Channel;
import org.ethereum.sync.SyncState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Process the messages between peers with 'eth' capability on the network<br>
 * Contains common logic to all supported versions
 * delegating version specific stuff to its descendants
 *
 */
public abstract class EthHandler extends SimpleChannelInboundHandler<EthMessage> implements Eth {

    private final static Logger logger = LoggerFactory.getLogger("net");

    protected Blockchain blockchain;

    protected SystemProperties config;

    protected CompositeEthereumListener ethereumListener;

    protected Channel channel;

    private MessageQueue msgQueue = null;

    protected EthVersion version;

    protected boolean peerDiscoveryMode = false;

    protected Block bestBlock;
    protected EthereumListener listener = new EthereumListenerAdapter() {
        @Override
        public void onBlock(Block block, List<TransactionReceipt> receipts) {
            bestBlock = block;
        }
    };

    protected int maxHashesAsk;

    protected boolean processTransactions = false;

    protected EthHandler(EthVersion version) {
        this.version = version;
    }

    protected EthHandler(final EthVersion version, final SystemProperties config,
                         final Blockchain blockchain, final BlockStore blockStore,
                         final CompositeEthereumListener ethereumListener) {
        this.version = version;
        this.config = config;
        this.ethereumListener = ethereumListener;
        this.blockchain = blockchain;
        maxHashesAsk = config.maxHashesAsk();
        bestBlock = blockStore.getBestBlock();
        this.ethereumListener.addListener(listener);
        // when sync enabled we delay transactions processing until sync is complete
//        processTransactions = !config.isSyncEnabled();
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, EthMessage msg) throws InterruptedException {

        if (EthMessageCodes.inRange(msg.getCommand().asByte(), version))
            logger.trace("EthHandler invoke: [{}]", msg.getCommand());

        ethereumListener.trace(String.format("EthHandler invoke: [%s]", msg.getCommand()));

        channel.getNodeStatistics().ethInbound.add();

        msgQueue.receivedMessage(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("Eth handling failed", cause);
        ctx.close();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        logger.debug("handlerRemoved: kill timers in EthHandler");
        ethereumListener.removeListener(listener);
        onShutdown();
    }

    public void activate() {
        logger.debug("ETH protocol activated");
        ethereumListener.trace("ETH protocol activated");
        sendStatus();
    }

    protected void disconnect(ReasonCode reason) {
        msgQueue.disconnect(reason);
        channel.getNodeStatistics().nodeDisconnectedLocal(reason);
    }

    protected void sendMessage(EthMessage message) {
        msgQueue.sendMessage(message);
        channel.getNodeStatistics().ethOutbound.add();
    }

    public StatusMessage getHandshakeStatusMessage() {
        return channel.getNodeStatistics().getEthLastInboundStatusMsg();
    }

    public void setMsgQueue(MessageQueue msgQueue) {
        this.msgQueue = msgQueue;
    }

    public void setPeerDiscoveryMode(boolean peerDiscoveryMode) {
        this.peerDiscoveryMode = peerDiscoveryMode;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public EthVersion getVersion() {
        return version;
    }

    @Override
    public synchronized boolean setStatus(SyncState syncState) {
        return false;
    }
}