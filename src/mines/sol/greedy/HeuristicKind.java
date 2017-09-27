package mines.sol.greedy;

/**
 * Enum for heuristic types.
 * Possible types are:
 * Minimise truck cycle time (MTCT),
 * Minimise truck return time (MTRT) (a variant of MTCT without crusher queueing time),
 * Minimise truck service time (MTST),
 * Minimise total truck waiting time (single stage) (MTTWT1) (a variant of MTWT that minimises waiting time up to filling),
 * Minimise total truck waiting time (double stage) (MTTWT2) (a variant of MTWT that minimises all waiting time in a cycle),
 * Minimise truck service waiting time (MTSWT) (close to the original definition of MTWT - minimise queueing time at the shovel),
 * Minimise shovel waiting time (MSWT).
 *
 * For more information on these greedy heuristics see:
 *
 * Tan, S., & Ramani, R. V. (1992, February). 
 * Evaluation of computer truck dispatching criteria. 
 * In Proceedings of the SME/AIME annual meeting and exhibition, Arizona (pp. 192-215).
 */
public enum HeuristicKind {
	MTRT, MTCT, MTST, MTTWT1, MTTWT2, MTSWT, MSWT
}