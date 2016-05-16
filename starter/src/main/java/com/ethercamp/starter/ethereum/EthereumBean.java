package com.ethercamp.starter.ethereum;

import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionInfo;
import org.ethereum.facade.Blockchain;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.mine.BlockMiner;
import org.ethereum.mine.Ethash;
import org.ethereum.mine.MinerListener;
import org.ethereum.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class EthereumBean {

    public Ethereum ethereum;
    public BlockMiner blockMiner;

    public void start(){
        this.ethereum = EthereumFactory.createEthereum();
        this.ethereum.addListener(new EthereumListener(ethereum));
    }

    public String getBestBlock(){
        return "" + ethereum.getBlockchain().getBestBlock().getNumber() + "\n"
                + ethereum.getBlockchain().getBestBlock().toString();
    }

    public String getBlockByNumber(int index){
        return ethereum.getBlockchain().getBlockByNumber(index).toString();
    }

    public String getBlockByHash(byte[] hash){
        return ethereum.getBlockchain().getBlockByHash(hash).toString();
    }

    public String getTransactionInfo(byte[] hash){
        TransactionInfo transactionInfo = ((org.ethereum.core.Blockchain)(ethereum.getBlockchain())).getTransactionInfo(hash);
        return transactionInfo.getReceipt().getTransaction().toString() + "\n"
                + transactionInfo.getReceipt().toString();
    }

    public List<Transaction> getTransactionHistory(byte[] receive){
        List<Transaction> transactionList = new LinkedList<>();
        Blockchain blockchain = ethereum.getBlockchain();

        for(long i=blockchain.getBestBlock().getNumber(); i>=0; --i){
            Block block = blockchain.getBlockByNumber(i);
            List<Transaction> blockTxList = block.getTransactionsList();

            for(int j=blockTxList.size()-1; j>=0; --j){
                Transaction tx = blockTxList.get(j);
                if(Arrays.equals(tx.getReceiveAddress(), receive)){
                    transactionList.add(tx);
                }
            }
        }

        return transactionList;
    }

    public List<Transaction> getTransactionHistoryTrack(byte[] receive){
        List<Transaction> transactionList = new LinkedList<>();
        Blockchain blockchain = ethereum.getBlockchain();

        for(long i=blockchain.getBestBlock().getNumber(); i>=0; --i){
            Block block = blockchain.getBlockByNumber(i);
            List<Transaction> blockTxList = block.getTransactionsList();

            for(int j=blockTxList.size()-1; j>=0; --j){
                Transaction tx = blockTxList.get(j);
                if(Arrays.equals(tx.getReceiveAddress(), receive)){
                    transactionList.add(tx);
                    receive = tx.getSender();
                }
            }
        }

        return transactionList;
    }

    public BlockMiner getBlockMiner(){
        return ethereum.getBlockMiner();
    }

    public void startMining(){
        if(blockMiner == null) {

            blockMiner = ethereum.getBlockMiner();
            blockMiner.addListener(new MinerListener() {
                @Override
                public void miningStarted() {
                    System.out.println("=== Miner.miningStarted");
                }

                @Override
                public void miningStopped() {
                    System.out.println("=== Miner.miningStopped");
                }

                @Override
                public void blockMiningStarted(Block block) {
                    System.out.println("=== Miner.blockMiningStarted " + blockInfo(block));
                }

                @Override
                public void blockMined(Block block) {
//                boolean validate = Ethash.getForBlock(block.getNumber()).validate(block.getHeader());
                    System.out.println("=== Miner.blockMined " + blockInfo(block));
//                System.out.println("=== MinerTest.blockMined: " + validate);
                }

                @Override
                public void blockMiningCanceled(Block block) {
                    System.out.println("=== Miner.blockMiningCanceled " + blockInfo(block));
                }
            });

        }

        Ethash.fileCacheEnabled = true;
        blockMiner.setFullMining(true);
        blockMiner.startMining();
    }

    public void stopMining() {
        if(blockMiner != null){
            blockMiner.stopMining();
        }
    }

    static String blockInfo(Block b) {
        boolean ours = Hex.toHexString(b.getExtraData()).startsWith("cccccccccc");
        String txs = "Tx[";
        for (Transaction tx : b.getTransactionsList()) {
            txs += ByteUtil.byteArrayToLong(tx.getNonce()) + ", ";
        }
        txs = txs.substring(0, txs.length() - 2) + "]";
        return (ours ? "##" : "  ") + b.getShortDescr() + " " + txs;
    }

}
