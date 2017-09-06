package search


data class Problem(
        val stateSpace: Graph,
        val initialNode: Node,
        val goalState: State) {

    init {
        stateSpace.getNode(initialNode.state)
    }

    constructor(stateSpace: Graph, initialState: State, goalState: State) : this(stateSpace, stateSpace.getNode(initialState), goalState)
}

data class Path(
        val states: List<State>,
        val cost: Double)
    : Comparable<Path>{

    override fun compareTo(other: Path): Int {
        // compare in this order:  path cost, end state, path length, all states
        val costCmp = cost.compareTo(other.cost)
        return if (costCmp == 0) {
            val endCmp = states[0].compareTo(other.states[0])
            if (endCmp == 0) {
                val lengthCmp = length.compareTo(other.length)
                if (lengthCmp == 0)
                    states.joinToString().compareTo(other.states.joinToString())
                else
                    lengthCmp
            } else
                endCmp
        } else
            costCmp
    }

    override fun toString() = states.joinToString(separator = ",", prefix = "<", postfix = ">")

    fun nextState() = states[0]

    fun didVisit(state: State) = states.contains(state)

    val length = states.size

    fun addState(state: State, edgeCost: Double): Path {
        return Path(listOf(state) + states, edgeCost + cost)
    }
}

interface IAlgorithm {
    fun search(problem: Problem, printExpansion: ((List<Path>) -> Unit)? = null): Boolean
    fun getName(): String
}

data class Algorithm(
        private val name: String,
        private val expansionOrder: Comparator<in State> = naturalOrder(),
        private val depthLimit: Int? = null,
        private val addToFringe: (List<Path>, Path) -> List<Path>)
    : IAlgorithm {

    // General_Search
    override fun search(problem: Problem, printExpansion: ((List<Path>) -> Unit)?): Boolean {
        // create fringe
        val root = Path(listOf(problem.initialNode.state), 0.0)
        val fringe = listOf(root)
        // recursively search and expand fringe
        return searchAndExpand(fringe, problem, printExpansion ?: {})
    }

    override fun getName() = name

    private fun searchAndExpand(fringe: List<Path>, problem: Problem, printExpansion: (List<Path>) -> Unit): Boolean {
        // check if goal not found
        if (fringe.isEmpty())
            return false

        // print expansion step
        printExpansion(fringe)

        // get the first state and path in the fringe
        val path = fringe[0]
        val restFringe = fringe.subList(1, fringe.size)
        val stateToExpand = path.nextState()

        // check if state is goal
        if (stateToExpand == problem.goalState)
            return true

        // expand the state, remove visited states, and sort by algorithm
        val children = problem.stateSpace
                .expandState(stateToExpand)
                .filterNot { path.didVisit(it) }
                .sortedWith(expansionOrder)

        // check depth limit if applicable
        val nextFringe = if (atDepthLimit(path)) restFringe else {

            // add a new path for each child to the fringe
            children.fold(restFringe) { newFringe, child ->
                val edgeCost = problem.stateSpace.costBetween(stateToExpand, child) ?: 0.0
                val newPath = path.addState(child, edgeCost)
                addToFringe(newFringe, newPath)
            }
        }

        // search next fringe
        return searchAndExpand(nextFringe, problem, printExpansion)
    }

    internal fun atDepthLimit(path: Path) =
        if (depthLimit != null)
           path.length > depthLimit
        else
            false
}



// SEARCHES



fun addToFront(fringe: List<Path>, path: Path) = listOf(path) + fringe

fun addToBack(fringe: List<Path>, path: Path) = fringe + listOf(path)


fun depthFirst() = Algorithm(
        name = "Depth 1st search",
        expansionOrder = reverseOrder(),
        addToFringe = ::addToFront
)

fun breadthFirst() = Algorithm(
        name = "Breadth 1st search",
        expansionOrder = naturalOrder(),
        addToFringe = ::addToBack
)

fun depthLimited(depth: Int) = Algorithm(
        name = "Depth-limited search (depth-limit = 2)",
        expansionOrder = reverseOrder(),
        addToFringe = ::addToFront,
        depthLimit = depth
)

fun iterativeDeepening() = object : IAlgorithm {
    override fun getName() = "Iterative deepening search"

    override fun search(problem: Problem, printExpansion: ((List<Path>) -> Unit)?): Boolean {

        val emptyOr = if (printExpansion == null) "" else null

        // repeat depth limited search with incrementing limit
        return generateSequence(0) { it + 1 }.any { limit ->

            print(emptyOr ?: "L=$limit")
            // run depth limited search with the current depth limit
            val success = depthLimited(limit).search(problem) { fringe ->
                val queueString = fringe.joinToString(" ", "[", "]")
                print(emptyOr ?: "   ${fringe[0].states[0]}      $queueString\n   ")
            }

            print(emptyOr ?: "\n")
            success
        }
    }
}

fun uniform() = Algorithm(
        name = "Uniform Search (Branch-and-bound)",
        expansionOrder = naturalOrder(),
        addToFringe = { fringe, path -> addToBack(fringe, path).sorted() }
)



// Todo Greedy, A*, Beam

