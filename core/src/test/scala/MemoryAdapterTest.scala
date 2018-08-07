import scala.collection.mutable._
import scala.util.control._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class MemoryAdapterTester(memadp:MemoryAdapter)extends PeekPokeTester(memadp){
	protected val mem=ArrayBuffer.fill[Long](0x100000)(0)
	//once again, why we can not have unsigned number in jvm?
	def getWord(address:Long):Long={
		val addr=address%0xFFFFFFFFL
		if(addr<0||addr>mem.length){
			return 0
		}
		(mem(addr)&(0xFFFFFFFFL))
	}
	def setWord(address:Long,data:Long)={
		val addr=address%0xFFFFFFFFL
		if(!(addr<0||addr>mem.length)){
			mem(addr)=(data&(0xFFFFFFFFL))
		}
	}
	def completeSubTask()={
		poke(memadp.io.raw.ready,false)
		val addr=peek(memadp.io.raw.addr).longValue
		val read=peek(memadp.io.raw.read).longValue
		val write=peek(memadp.io.raw.write).longValue
		expect((read==0)|(write==0),"no read and write at the same time")
		if(read!=0){
			val data=getWord(addr)
			step(1)
			println(f"----[#load]from 0x$addr%x get 0x$data%x")
			poke(memadp.io.raw.rdata,data)
		}
		if(write!=0){
			val data=peek(memadp.io.raw.wdata).longValue
			step(1)
			println(f"----[#store]to 0x$addr%x set 0x$data%x")
			setWord(addr,data)
		}
		poke(memadp.io.raw.ready,true)
	}
	def completeTask()={
		if(peek(memadp.io.core.ready)==0){
			import Breaks._
			breakable{
				while(true){
					completeSubTask()
					if(peek(memadp.io.core.ready)!=0){
						break
					}
					step(1)
				}
			}
		}
	}
	import MemOpSignal._
	import MemWidthSignal._
	poke(memadp.io.raw.ready,false)
	// only i
	poke(memadp.io.core.iaddr,0)
	poke(memadp.io.core.access,false)
	completeTask()
	expect(memadp.io.core.idata,0)
	step(1)
	// store not align
	poke(memadp.io.core.access,true)
	poke(memadp.io.core.rwtype,M_STORE)
	poke(memadp.io.core.rwwidth,W_W)
	poke(memadp.io.core.rwaddr,2)
	poke(memadp.io.core.wdata,0x12345678L)
	completeTask()
	expect(memadp.io.core.idata,0x56780000L)
	step(1)
	// read not align
	poke(memadp.io.core.rwtype,M_LOAD)
	poke(memadp.io.core.rwwidth,W_H)
	poke(memadp.io.core.rwaddr,3)
	poke(memadp.io.core.iaddr,3)
	completeTask()
	expect(memadp.io.core.idata,0x00123456L)
	expect(memadp.io.core.rdata,0x00003456L)
}

class MemoryAdapterTest extends ChiselFlatSpec{
	"MemoryAdapter" should "works" in {
		iotesters.Driver.execute(Array("--is-verbose"),()=>new MemoryAdapter){
			c=>new MemoryAdapterTester(c)
		}should be(true)
	}
}