import java.util.HashMap;

public class KVStore {

    private HashMap<String, String> kv;

    public KVStore() {
        this.kv = new HashMap<>();
    }

    public void put(String key, String val) {
        this.kv.put(key, val);
    }

    public void del(String key) {
        this.kv.remove(key);
    }

    public String get(String key) {
        return this.kv.get(key);
    }
}
