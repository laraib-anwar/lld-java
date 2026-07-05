import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// ==========================================
// 1. Core Enums and Models
// ==========================================
enum IssueType {
    PAYMENT, ACCOUNT, MUTUAL_FUNDS
}

enum IssuePriority {
    LOW, MEDIUM, HIGH
}

enum IssueState {
    OPEN, ASSIGNED, RESOLVED
}

class Issue {
    private final String id;
    private final String description;
    private final IssueType type;
    private final IssuePriority priority;
    private IssueState state;
    private Agent assignedAgent;

    public Issue(String id, String description, IssueType type, IssuePriority priority) {
        this.id = id;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.state = IssueState.OPEN;
    }

    // Thread-safe state modifications
    public synchronized void assignTo(Agent agent) {
        if (this.state != IssueState.OPEN) {
            throw new IllegalStateException("Issue cannot be assigned from state: " + this.state);
        }
        this.assignedAgent = agent;
        this.state = IssueState.ASSIGNED;
    }

    public synchronized void resolveIssue() {
        if (this.state != IssueState.ASSIGNED) {
            throw new IllegalStateException("Only assigned issues can be marked resolved.");
        }
        this.state = IssueState.RESOLVED;
        if (this.assignedAgent != null) {
            this.assignedAgent.setAvailable(true);
        }
    }

    // Getters
    public String getId() { return id; }
    public IssueType getType() { return type; }
    public IssuePriority getPriority() { return priority; }
    public IssueState getState() { return state; }
    public Agent getAssignedAgent() { return assignedAgent; }
}

class Agent {
    private final String id;
    private final String name;
    private final IssueType expertise;
    private boolean isAvailable;

    public Agent(String id, String name, IssueType expertise) {
        this.id = id;
        this.name = name;
        this.expertise = expertise;
        this.isAvailable = true;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public IssueType getExpertise() { return expertise; }
    public synchronized boolean isAvailable() { return isAvailable; }
    public synchronized void setAvailable(boolean available) { this.isAvailable = available; }
}

// ==========================================
// 2. Central Orchestrator Service
// ==========================================
class ResolutionSystem {
    // PriorityQueue sorts issues by Priority level (HIGH down to LOW)
    private final PriorityQueue<Issue> issueQueue;
    private final Map<String, Agent> agentRegistry;
    private final Map<String, Issue> allIssues;

    public ResolutionSystem() {
        // High priority sorted first; if priorities match, sort by ID sequence order
        this.issueQueue = new PriorityQueue<>((a, b) -> {
            if (a.getPriority() != b.getPriority()) {
                return b.getPriority().compareTo(a.getPriority()); 
            }
            return a.getId().compareTo(b.getId());
        });
        this.agentRegistry = new ConcurrentHashMap<>();
        this.allIssues = new ConcurrentHashMap<>();
    }

    public void registerAgent(Agent agent) {
        agentRegistry.put(agent.getId(), agent);
    }

    public void createIssue(String id, String desc, IssueType type, IssuePriority priority) {
        Issue issue = new Issue(id, desc, type, priority);
        allIssues.put(id, issue);
        issueQueue.add(issue);
        System.out.println("📥 Ticket Logged: [" + id + "] - " + desc + " (Priority: " + priority + ")");
    }

    /**
     * Core Algorithm: Evaluates pending tickets and matches them against free, skilled agents.
     */
    public synchronized void processNextIssue() {
        if (issueQueue.isEmpty()) {
            System.out.println("ℹ️ No pending issues in the queue.");
            return;
        }

        // Peek at the highest priority issue to see if we can fulfill it
        Issue targetIssue = issueQueue.peek();
        Agent matchingAgent = findAvailableAgent(targetIssue.getType());

        if (matchingAgent == null) {
            System.out.println("⏳ Matching Agent currently busy for issue type: " + targetIssue.getType() + ". Retrying later.");
            return;
        }

        // Dequeue ticket and map the relationship
        issueQueue.poll();
        matchingAgent.setAvailable(false);
        targetIssue.assignTo(matchingAgent);

        System.out.println("⚙️ Ticket [" + targetIssue.getId() + "] successfully assigned to Agent: " + matchingAgent.getName());
    }

    public void completeIssue(String issueId) {
        Issue issue = allIssues.get(issueId);
        if (issue != null) {
            issue.resolveIssue();
            System.out.println("✅ Ticket [" + issueId + "] has been marked RESOLVED.");
        }
    }

    private Agent findAvailableAgent(IssueType type) {
        for (Agent agent : agentRegistry.values()) {
            if (agent.isAvailable() && agent.getExpertise() == type) {
                return agent;
            }
        }
        return null;
    }
}

// ==========================================
// 3. Main Execution Driver
// ==========================================
public class PhonePeCustomerService {
    public static void main(String[] args) {
        ResolutionSystem phonePeSupport = new ResolutionSystem();

        // Step 1: Onboard agents with varying expertise domains
        phonePeSupport.registerAgent(new Agent("A1", "Amit (Payments Expert)", IssueType.PAYMENT));
        phonePeSupport.registerAgent(new Agent("A2", "Suresh (Account Expert)", IssueType.ACCOUNT));

        System.out.println("=== PHONEPE CRM OPERATIONAL ===\n");

        // Step 2: Customers file issues out of order
        phonePeSupport.createIssue("T1", "Money deducted but transaction failed", IssueType.PAYMENT, IssuePriority.MEDIUM);
        phonePeSupport.createIssue("T2", "Unable to change linked bank account", IssueType.ACCOUNT, IssuePriority.HIGH); // Higher Priority
        phonePeSupport.createIssue("T3", "Payment frozen midway during scanning", IssueType.PAYMENT, IssuePriority.HIGH);

        System.out.println("\n--- RUNNING MATCHING ENGINE ---");
        
        // Step 3: Run processing passes. T2 and T3 should process ahead of T1 due to higher priority levels.
        phonePeSupport.processNextIssue(); // Processes T2 (Assigned to Suresh)
        phonePeSupport.processNextIssue(); // Processes T3 (Assigned to Amit)
        phonePeSupport.processNextIssue(); // T1 fails allocation because Amit is now busy handling T3

        System.out.println("\n--- RESOLVING ACTIVE TICKETS ---");
        phonePeSupport.completeIssue("T3"); // Amit frees up

        System.out.println("\n--- RETRYING PENDING QUEUE ---");
        phonePeSupport.processNextIssue(); // T1 can now be successfully assigned to Amit!
    }
}