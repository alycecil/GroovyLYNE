/* 
Attempts to solve a modified graph traversal

Rules:
Each node may only be traveled to once if it is lettered, 
    if it is numbered it must be traveled to that many times
Each node connects only to its neighboring nodes
Each node to node transfer has a distance 1 (all distances between neigboring nodes is equal)
Each node must be traveled to its required number of times

Each lettered nodes must have two root nodes, denoted in capital letters, the traversal 
must start at one root node and end at the other. If a path encounters an end node, it must 
have first encountered all other nodes of its type.

No paths may cross except at nodes; which is only relavent for diagonal paths.

A graph is considered traveresed when all nodes have reached their required number of visits

Key features:
Not all graphs are solvable. 
There may not be a unique solution


Ex 
A a A
 solves to 
A -> a - > A
 or 
A <- a <- A

Ex 
A 2 A
0 B B

Has a solution like
A>2>A
  |\
0 B B


*/

/*
rev 2
- Does not support common nodes yet
*/

import groovy.transform.Canonical
import groovy.transform.TupleConstructor
import groovy.transform.EqualsAndHashCode




/**
* identity
*/
class SharedClosures {
    /*
    * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    * Generic Callables
    * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    */

    /**
    * identity
    */
    static def I = {
        world, x, y->
        

        world[y][x]
    }

	//static def seed = 1424834035623l
	static def seed = System.currentTimeMillis()
	static def random = new java.util.Random(seed)

	static def debug = false
}


def forEachAsArray(world , callable){
    def result = []
    (0..world.size-1).each {
        y -> 
        (0..world[y].size-1).each {
            x -> 
            def res = callable(world, x , y)
            if(res)
                result << res
        }
    }

    result
}


@Canonical(excludes= 'neighbors')
abstract class Node {
    def movesFrom
    def movesTo

    def x
    def y
    int count = 0
    int needs = 1

    def neighbors

    def isComplete(){
        if(count>1){
            throw new IllegalStateException("$this@($x,$y) Too many hits [$count], bug in code!")
        }
        
        count == needs
    }

    def isValid(){
    	//println "Node.isValid ${needs>0}+${true&&movesTo}+${true&&movesFrom}"
    	return needs>0&&movesTo&&movesFrom;
    }
}

@TupleConstructor(includeSuperProperties=true) 
@Canonical(excludes= 'neighbors')
class CommonNode extends Node {

}

@TupleConstructor(includeSuperProperties=true) 
@Canonical(excludes= 'neighbors,otherRoot')
class SingleNode extends Node {
    def type

    def isValid(){
    	//println "SingleNode.isValid ${type!=null}"
    	return super.isValid()&&type!=null
    }
}

@TupleConstructor(includeSuperProperties=true) 
@Canonical(excludes= 'neighbors,twin')
class RootNode extends SingleNode {
    def twin

    def isValid(){
    	return super.isValid()&&twin!=null
    }
}

class Move {
    def invalidatedBy
    def invalidates
    def invalidatedConditionally

    Node source, destination;

    String toString(){
        def s = "${this.class.getName()} from ($source(${source.x},${source.y})) "
        s += "to ($destination (${destination.x},${destination.y}))"

        s
    }
}

class DiagonalMove extends Move {
}

class Action{
    def make
    def move

    String toString(){
        def s = "${this.class}($make) $move"

        s
    }
}


/*******************************
GENERIC METHODS TO BUILD MY WORLD
*/

def buildWorld(def argFile){
    def worldText = []
    

    new File(argFile).eachLine {
        def worldLine = []
        it.getChars().each {
            //
            worldLine << it
        }
        worldText << worldLine
    }

    def world = []

    def starts = [:]

    //first pass, build
    (0..worldText.size-1).each {
        y -> 
        def worldCol = [];
        (0..worldText[y].size-1).each {
            x -> 

            def val = worldText[y][x]
            def node = null

            if(Character.isDigit( val )){
                val = Integer.parseInt( "$val" )
                if(val>0){
                    node = new CommonNode(needs: val)
                }
            } else {
                //
                if(Character.isUpperCase( val )){
					node = new RootNode()
					node.type= val.toLowerCase() 

					def twin = starts.get(val)
                	if(twin==null){
						starts.put(val,node)

						if(SharedClosures.debug) println "Twin stored for $node of type $val"
            		}else{
                		twin.twin = node	
                		node.twin = twin	
                		if(SharedClosures.debug) println "Twin set for $node & $twin of type $val"
            		}
                }else{
                	node = new SingleNode(type: val.toLowerCase() )
           		}
            }

            if(node!=null){
                node.x = x
                node.y = y
            }
            worldCol << node
        }

        world << worldCol;

    }

    world
}


def forEachNeighborOfPosition(world, x, y, callThis){
    if(world[y][x]!=null){
        (y-1..y+1).each {
            _y->
            if(!(_y < 0 || _y > world.size-1)){
                (x-1..x+1).each {
                    _x ->
                    if(_x < 0 || _x > world[_y].size-1 || (_x == x && _y == y)){
                        //continue       
                    } else if(world[_y][_x]!=null) {
                        //add as neighbor
                        //world[y][x].neighbors << world[_y][_x] 
                        callThis(world, x, y, _x, _y)
                    }
                }
            }
        }
    }
}
def forEachNeighbor(world, callThis){
    (0..world.size-1).each {
        y -> 

        (0..world[y].size-1).each {
            x -> 

            forEachNeighborOfPosition(world, x, y, callThis)
            
        }
    }
}

void buildNeighbors(world){
	def makeNeighbors = {
        w, mx, my, m_x, m_y ->
        if(w[my][mx].neighbors == null){
            w[my][mx].neighbors = []
            if(SharedClosures.debug) println ''
            if(SharedClosures.debug) println "working on ($mx, $my)"
        }

        w[my][mx].neighbors << w[m_y][m_x] 

        if(SharedClosures.debug) print "${w[my][mx].neighbors.size} "
    }
    
    //next pass build neighbors in each node
    forEachNeighbor(world, makeNeighbors)
    if(SharedClosures.debug) println ''
}

def buildMoves(world){
	def fog = {
		me->
			me.movesTo = [] as Set
			me.movesFrom = [] as Set
			def moves = [] as List
			me?.neighbors?.each {
				 if(me.x!=it.x && me.y!=it.y){
			        //diagonal move
			        def move = new DiagonalMove(source: me, destination: it)

			        moves << move
			    }else{
			        moves << new Move(source: me, destination: it)
			    }
			}

			moves
		} << SharedClosures.I

	def moves = []
	forEachAsArray(world, fog).each {
		moves.addAll(it)
	}

	moves.each{
		it.invalidates = [] as Set
		it.invalidatedBy = [] as List
		it.source.movesFrom << it
		it.destination.movesTo << it
	}

	moves.each{
		//no other move can come from here (No common nodes)
		it.source.movesFrom.each {
			me ->
				it.invalidates << me
		}

		if(it.source instanceof RootNode){
			it.source.movesTo.each {
				me ->
				it.invalidates << me
			}

			//my twins from moves are gone too
	    	it.source.twin.movesFrom.each {
	    		me ->
	    		it.invalidates << me
	    	}
		}

		if(it.destination instanceof RootNode){
			it.destination.movesTo.each {
				me ->
				it.invalidates << me
			}

			it.destination.twin.movesTo.each{
				me ->
	    		it.invalidates << me
			}
		}

		//find reverse
		it.destination.movesFrom.each{
			reverse ->
				if(reverse.destination == it.source){
					it.invalidates << reverse
				}
		}

		if(it instanceof DiagonalMove){
			if(SharedClosures.debug) println "removing paired diags for $it"
			def point1 = world[it.source.y][it.destination.x]
            def point2 = world[it.destination.y][it.source.x]

            if(point1==null){
                if(SharedClosures.debug) println "counterpoints at ${it.x},${me.y} is null"
            }else if(point2==null){
                if(SharedClosures.debug) println "counterpoints at ${me.x},${it.y} is null"
            }else{
                point1.movesFrom?.each {
                    dm ->
                    if(dm.destination.is(point2)){
                    	if(SharedClosures.debug) println "Diag : (${dm.source.x},${dm.source.y}) to (${dm.destination.x},${dm.destination.y})"
                        it.invalidates << dm
                    }
                }

                point2.movesFrom?.each {
                    dm ->
                    if(dm.destination.is(point1)){
                    	if(SharedClosures.debug) println "Diag : (${dm.source.x},${dm.source.y}) to (${dm.destination.x},${dm.destination.y})"
                        it.invalidates << dm
                    }
                }
            }
		}
	}

	moves
}

/*********************************
Actual Game
*/

boolean hasWon(nodeList){
    boolean hasWon = true

    nodeList.each{
        node ->

        hasWon = hasWon && node.isComplete()
    }

    hasWon
}

def makeMove(move, movesMade, movesAvailable, nodeList){
	movesAvailable.remove(move)
    movesMade << move

   
    if(move.source instanceof RootNode){
    	//I am complete when you leave me
    	move.source.count++

    	if(!move.source.isComplete()){
    		throw new IllegalStateException('Bug! root not complete on exit')
    	}
    }

    //make all as invalidated
    move.invalidates.each{
    	it.invalidatedBy << move

    	movesAvailable.remove(it)
    }

    move.destination.count ++

    if(move.destination.isComplete()){
    	
    	move.invalidatedConditionally = [] as Set

    	move.destination.movesTo.each{

    		move.invalidatedConditionally << it
    	}

    	if(move.destination instanceof RootNode){
    		if(!move.destination.isComplete()){
    			throw new IllegalStateException('Bug! root not complete on enter')
    		}
   		}
    }

	move.invalidatedConditionally.each{
    	it.invalidatedBy << move

    	movesAvailable.remove(it)
    }


    if(SharedClosures.debug) println "moves ${movesAvailable.size()}"
    if(SharedClosures.debug) movesAvailable.each {
    	println "     $it"
    }
}

def unmakeMove(move, movesMade, movesAvailable, nodeList){
	movesMade.remove(move)
	movesAvailable << move

	move.invalidatedConditionally?.each {
		it.invalidatedBy.remove(move)

		if(!it.invalidatedBy){
			if(SharedClosures.debug) println "re-add-cond $it"
			movesAvailable << it
		}
	}
	move.invalidatedConditionally = null

	move.destination.count--

	move.invalidates?.each{
    	it.invalidatedBy.remove(move)

    	if(!it.invalidatedBy){
    		if(SharedClosures.debug) println "re-add      $it"
			movesAvailable << it
		}
    }

    if(move.source instanceof RootNode){
    	move.source.count--
    }
}

def solve(world, rootMoves){
	def nodeList = forEachAsArray(world, SharedClosures.I)
	def movesMade = [] as List
	def movesAvailable = [] as Set
    def round = 0;
    def lastAction = null

	movesAvailable.addAll(rootMoves);

    while(!hasWon(nodeList)){
        
        def action = chooseAction(world, nodeList, movesMade, movesAvailable, lastAction)

        println "$round:$action"

        if(action.make){
            makeMove(action.move, movesMade, movesAvailable, nodeList)
            
        }else{
            unmakeMove(action.move, movesMade, movesAvailable, nodeList)
        }

        round ++

        lastAction = action
    }

    println 'This would be the victory dance!'
    println "$movesMade"
}


/**
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
* The interesting method
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*/

def chooseAction(world, nodeList, movesMade, movesAvailable, lastAction){
	def SCALE = 100;
	def a = new Action();

	if(movesAvailable){
		//do i step forward or back?
		def makeMove = true;

		if(movesMade){
			if(SharedClosures.random.nextInt()%1000>=SCALE*movesAvailable.size()/movesMade.size()){
				makeMove = false;
			}
		}

		if(makeMove){

			a.make = true
			def ind = Math.abs(SharedClosures.random.nextInt()%movesAvailable.size())
			a.move = movesAvailable[ind]

			println "Choosing $ind of ${movesAvailable.size()}"

			//no looping
			if(lastAction&&movesMade&&a.move==lastAction.move){
				println 'NO LOOPING'
				a.move = null
			}
		}
	}//else{//roll back}

	if(!a.move){
		println 'Roll back'
        a.make = false

        a.move = movesMade[movesMade.size-1]
    }

    a
}

/**
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
* Args
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*/
def argFile = 'C:\\Users\\Courtney Cecil\\Documents\\groovy\\world.txt'//this.args[0]

/**
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
* MAIN
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*/
def world = buildWorld(argFile)
buildNeighbors(world)
def moves = buildMoves(world)
println 'Build Complete'


//composition
println '---All nodes---'
def mud =  {println it} << SharedClosures.I
forEachAsArray(world,mud)


//
println '---All moves---'
def rootMoves = []
moves.each{
	

	if(it?.source instanceof RootNode){
		rootMoves << it

		println "*$it*"
	}else {
		println it
	}
}

println '--- Valid? ---'
mud = {if(!it.isValid()){throw new IllegalStateException("$it@(${it.x},${it.y}) is not valid")}} << SharedClosures.I
forEachAsArray(world,mud)
if(!rootMoves){
	throw new IllegalStateException('No root moves');
}
println 'Yes.'
println '---------------'
println "Seed : ${SharedClosures.seed}"

solve(world, moves)

