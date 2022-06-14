package Bean;

import java.math.BigDecimal;

public class MyTransaction {
    private String Txid;
    private String status;
    private String timestamp;
    private BigDecimal amount;
    private String address;
    private String foods;

    public MyTransaction(String txid, String status, String timestamp, BigDecimal amount, String address, String foods) {
        this.Txid = txid;
        this.status = status;
        this.timestamp = timestamp;
        this.amount = amount;
        this.address = address;
        this.foods = foods;
    }


    @Override
    public String toString() {
        return "MyTransaction{" +
                "Txid='" + Txid + '\'' +
                ", status='" + status + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", amount=" + amount +
                ", address='" + address + '\'' +
                ", foods='" + foods + '\'' +
                '}';
    }

    public String getTxid() {
        return Txid;
    }

    public void setTxid(String txid) {
        Txid = txid;
    }

    public String getFoods() {
        return foods;
    }

    public void setFoods(String foods) {
        this.foods = foods;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getAddress() {
        return address;
    }
}
