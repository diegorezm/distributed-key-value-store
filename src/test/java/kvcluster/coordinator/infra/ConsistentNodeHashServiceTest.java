package src.test.java.kvcluster.coordinator.infra;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import src.main.java.kvcluster.coordinator.infra.ConsistentNodeHashService;

@Tag("unit")
public class ConsistentNodeHashServiceTest {

    @Test
    void routeForThrowsWhenRingIsEmpty() {
        var router = new ConsistentNodeHashService();

        assertThrows(IllegalStateException.class, () ->
            router.routeFor("alpha", 1)
        );
    }

    @Test
    void singleNodeAlwaysOwnsTheKey() {
        var router = new ConsistentNodeHashService();
        router.addNode("node-1");

        assertEquals("node-1", router.routeFor("alpha"));
        assertIterableEquals(List.of("node-1"), router.routeFor("alpha", 3));
        assertIterableEquals(List.of("node-1"), router.routeFor("beta", 2));
    }

    @Test
    void sameKeyProducesSameRoute() {
        var router = new ConsistentNodeHashService();
        router.addNode("node-1");
        router.addNode("node-2");
        router.addNode("node-3");

        var first = router.routeFor("customer:42", 3);
        var second = router.routeFor("customer:42", 3);

        assertIterableEquals(first, second);
    }

    @Test
    void routeContainsDistinctPhysicalNodes() {
        var router = new ConsistentNodeHashService();
        router.addNode("node-1");
        router.addNode("node-2");
        router.addNode("node-3");

        var route = router.routeFor("alpha", 3);

        assertEquals(3, route.size());
        assertEquals(3, route.stream().distinct().count());
    }

    @Test
    void removedNodeIsNeverReturned() {
        var router = new ConsistentNodeHashService();
        router.addNode("node-1");
        router.addNode("node-2");
        router.addNode("node-3");
        router.removeNode("node-2");

        for (String key : List.of("a", "b", "c", "d", "e")) {
            assertFalse(router.routeFor(key, 3).contains("node-2"));
        }
    }

    @Test
    void routeForNeverReturnsMoreDistinctNodesThanExist() {
        var router = new ConsistentNodeHashService();
        router.addNode("node-1");
        router.addNode("node-2");

        var route = router.routeFor("alpha", 5);

        assertEquals(2, route.size());
        assertEquals(2, route.stream().distinct().count());
    }
}
