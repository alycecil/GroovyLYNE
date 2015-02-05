import groovy.transform.Canonical
import groovy.transform.TupleConstructor

@Canonical(excludes= 'neighbors')
class Node {
    def neighbors
    def x
    def y
}

@TupleConstructor(includeSuperProperties=true) 
@Canonical(excludes= 'neighbors')
class CommonNode extends Node{
    short count
    short hits
}

@TupleConstructor(includeSuperProperties=true) 
@Canonical(excludes= 'neighbors')
class SingleNode extends Node {
    char type
    boolean start
}

def calcNeighbors(def argFile){
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
                    node = new CommonNode(count: val, hits: 0)
                }
            } else {
                //
                node = new SingleNode(type: val.toLowerCase(), start: Character.isUpperCase( val ) )
            }

            if(node!=null){
                node.x = x
                node.y = y
            }
            worldCol << node
        }

        world << worldCol;

    }


    //next pass build neighbors in each node
    (0..worldText.size-1).each {
        y -> 

        (0..worldText[y].size-1).each {
            x -> 
            if(world[y][x]!=null){
                world[y][x].neighbors = []

                (y-1..y+1).each {
                    _y->
                    if(_y < 0 || _y > worldText.size-1){
                        //continue        
                    }else{
                        (x-1..x+1).each {
                            _x ->
                            if(_x < 0 || _x > worldText[_y].size-1 || (_x == 0 && _y == 0)){
                                //continue       
                            } else if(world[_y][_x]!=null) {
                                //add as neighbor
                                world[y][x].neighbors << world[_y][_x] 
                            }
                        }
                    }
                }
            }
        }
    }

    world
}

def validate(world){
    def cnt = [:]
    (0..world.size-1).each {
        y -> 

        (0..world[y].size-1).each {
            x -> 
            //
            if(world[y][x] instanceof SingleNode){

                if(world[y][x].start){

                    if(cnt[world[y][x].type]==null){
                        cnt[world[y][x].type]=1
                    }else if(cnt[world[y][x].type]==1){
                        cnt[world[y][x].type]=2
                    }else{
                        throw IllegalStateException()
                    }
                }
            }
        }
    }
}


def calcAsMap(world , callable){
    def result = []
    (0..world.size-1).each {
        y -> 
        def resLine = []
        (0..world[y].size-1).each {
            x -> 
            resLine << callable(world[y][x]);
        }

        result << resLine
    }

    result
}

def calcAsArray(world , callable){
    def result = []
    (0..world.size-1).each {
        y -> 
        (0..world[y].size-1).each {
            x -> 
            def res = callable(world[y][x])
            if(res)
                result << res
        }
    }

    result
}

def callThis = {
    it->
    

    it
}

def argFile = 'C:\\Users\\Courtney Cecil\\Documents\\groovy\\world.txt'//this.args[0]
def res = calcNeighbors(argFile)
if(!validate(res)){
    throw new IllegalStateException()
}

res2 = calcAsMap(res, callThis)
println '- - - -'

res2.each{
    worldLine ->
    
    worldLine.each {
        ch ->
        print " $ch "
    }
    println ''
}

println '- - - -'

def res3 = calcAsArray(res, callThis)

res3.each {
    println it
}







