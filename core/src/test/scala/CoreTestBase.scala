import scala.collection.mutable._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class CoreTesterBase(core:Core)extends PeekPokeTester(core){
	//we use only the lower half bits, why we can not have unsigned number in jvm?
	protected val mem=ArrayBuffer.fill[Short](0x800000)(0)
	//init mem
	protected def getByte(address:Long):Short={
		val addr=address%0x3FFFFFFFFL
		if(address<0||address>mem.length){
			return 0
		}
		(mem(address)&(0xFF.shortValue)).shortValue
	}
	protected def setByte(address:Long,data:Short)={
		val addr=address%0x3FFFFFFFFL
		if(!(address<0||address>mem.length)){
			mem(address)=(data&(0xFF)).shortValue
		}
	}
	protected def getHalf(address:Long):Int={
		(getByte(address+1).intValue<<8)+getByte(address).intValue
	}
	protected def setHalf(address:Long,data:Int)={
		setByte(address,(data&0xFF).shortValue)
		setByte(address+1,((data>>8)&0xFF).shortValue)
	}
	protected def getWord(address:Long):Long={
		(getHalf(address+2).longValue<<16)+getHalf(address).longValue
	}
	protected def setWord(address:Long,data:Long)={
		setHalf(address,(data&0xFFFF).intValue)
		setHalf(address+2,((data>>16)&0xFFFF).intValue)
	}
	protected def loadInst(insts:Iterable[Long])={
		for((inst,id)<- insts.zipWithIndex){
			setWord(id*4,inst)
		}
	}
	protected def stepStage()={
		poke(core.io.mem.ready,false)
		val iaddr=peek(core.io.mem.iaddr).longValue
		val access=peek(core.io.mem.access).longValue
		if(access>0){
			step(1)
			val rwtype=peek(core.io.mem.rwtype).longValue
			val rwaddr=peek(core.io.mem.rwaddr).longValue
			val rwwidth=peek(core.io.mem.rwwidth).longValue
			rwtype match{
				case 0=>{//load
					val rdata=rwwidth match{
						case 0=>getByte(rwaddr).longValue
						case 1=>getHalf(rwaddr).longValue
						case 2=>getWord(rwaddr).longValue
					}
					println(f"----[#load]from 0x$rwaddr%x get 0x$rdata%x (width: ' $rwwidth%x ')")
					poke(core.io.mem.rdata,rdata)
				}
				case 1=>{//store
					val wdata=peek(core.io.mem.wdata)
					println(f"----[#store]to 0x$rwaddr%x set 0x$wdata%x (width: ' $rwwidth%x ')")
					rwwidth match{
						case 0=>setByte(rwaddr,wdata.shortValue)
						case 1=>setHalf(rwaddr,wdata.intValue)
						case 2=>setWord(rwaddr,wdata.longValue)
					}
				}
			}
		}
		poke(core.io.mem.idata,getWord(iaddr))
		poke(core.io.mem.ready,true)
		step(1)
	}
	protected def stepStages(times:Int=1)={
		for(i<-0 until times){
			stepStage()
		}
	}
}

class CoreTestBase(coreTesterGen:(Core)=>CoreTesterBase,verbose:Boolean=true) extends ChiselFlatSpec{
	"Core" should "works" in {
		iotesters.Driver.execute((if(verbose){Array("--is-verbose")}else{Array()}),()=>new Core){
			coreTesterGen
		}should be(true)
	}
}
