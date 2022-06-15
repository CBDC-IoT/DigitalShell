package Bean;

import java.math.BigDecimal;

public class MyTransaction {
    private String Txid;
    private String status;
    private String timestamp;
    private BigDecimal amount;
    private String payeeNode;
    private String PayeeAddress;
    private String item;

    public MyTransaction(String txid, String status, String timestamp, BigDecimal amount, String payeeNode, String payeeAddress, String item) {
        Txid = txid;
        this.status = status;
        this.timestamp = timestamp;
        this.amount = amount;
        this.payeeNode = payeeNode;
        PayeeAddress = payeeAddress;
        this.item = item;
    }

    @Override
    public String toString() {
        return "MyTransaction{" +
                "Txid='" + Txid + '\'' +
                ", status='" + status + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", amount=" + amount +
                ", payeeNode='" + payeeNode + '\'' +
                ", PayeeAddress='" + PayeeAddress + '\'' +
                ", item='" + item + '\'' +
                '}';
    }

    public String getTxid() {
        return Txid;
    }

    public void setTxid(String txid) {
        Txid = txid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPayeeNode() {
        return payeeNode;
    }

    public void setPayeeNode(String payeeNode) {
        this.payeeNode = payeeNode;
    }

    public String getPayeeAddress() {
        return PayeeAddress;
    }

    public void setPayeeAddress(String payeeAddress) {
        PayeeAddress = payeeAddress;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }
}
