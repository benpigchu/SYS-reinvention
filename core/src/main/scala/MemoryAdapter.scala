package reinventation_core

import chisel3._
import chisel3.util._

class RawMemIO extends Bundle{
	val ready=Input(Bool())
	val addr=Output(UInt(32.W))
	val wdata=Output(UInt(32.W))
	val rdata=Input(UInt(32.W))
	val read=Output(Bool())
	val write=Output(Bool())
}

class MemoryAdapterIO extends Bundle{
	val core=Flipped(new CoreMemIO)
	val raw=new RawMemIO
}

class MemoryAdapter extends Module{
	val io=IO(new MemoryAdapterIO)
	class Task extends Bundle{
		val iaddr=UInt(34.W)
		val access=Bool()
		val rwtype=UInt(MemOpSignal.width)
		val rwaddr=UInt(34.W)
		val wdata=UInt(32.W)
		val rwwidth=UInt(MemWidthSignal.width)
	}
	val coreTask=Wire(new Task)
	coreTask.iaddr:=io.core.iaddr
	coreTask.access:=io.core.access
	coreTask.rwtype:=io.core.rwtype
	coreTask.rwaddr:=io.core.rwaddr
	coreTask.wdata:=io.core.wdata
	coreTask.rwwidth:=io.core.rwwidth
	val fullReady=Wire(Bool())
	val taskUpdate=RegInit(true.B)
	taskUpdate:=fullReady
	io.core.ready:=fullReady
	val cachedRequest=RegInit(0.U.asTypeOf(coreTask))
	val task=Mux(taskUpdate,coreTask,cachedRequest)
	cachedRequest:=task
	import MemOpSignal._
	import MemWidthSignal._
	//helper
	val rwNextNeeded=((task.rwwidth===W_H)&(task.rwaddr(1)===1.U(1.W)))|((task.rwwidth===W_W)&(task.rwaddr(1,0)=/=0.U(2.W)))
	val readBeforeWriteNeeded=(!((task.rwwidth===W_W)&(task.rwaddr(1,0)===0.U(2.W))))
	val readNeeded=task.access&(task.rwtype===M_LOAD)
	val writeNeeded=task.access&(task.rwtype===M_STORE)
	//subtask needed
	val readBaseNeeded=readNeeded|(writeNeeded&readBeforeWriteNeeded)
	val readNextNeeded=readBaseNeeded&rwNextNeeded
	val writeBaseNeeded=writeNeeded
	val writeNextNeeded=writeBaseNeeded&rwNextNeeded
	val instBaseNeeded=true.B
	val instNextNeeded=(task.iaddr(1,0)=/=0.U(2.W))
	//address
	val baseRwaddr=task.rwaddr(33,2)
	val nextRwaddr=baseRwaddr+1.U
	val baseIaddr=task.iaddr(33,2)
	val nextIaddr=baseIaddr+1.U
	//current task status
	//use cached ones to determine current task
	//use not cached ones to determine fullReady
	val readBaseReadyCache=RegInit(false.B)
	val readBaseReady=Wire(Bool())
	val readNextReadyCache=RegInit(false.B)
	val readNextReady=Wire(Bool())
	val writeBaseReadyCache=RegInit(false.B)
	val writeBaseReady=Wire(Bool())
	val writeNextReadyCache=RegInit(false.B)
	val writeNextReady=Wire(Bool())
	val instBaseReadyCache=RegInit(false.B)
	val instBaseReady=Wire(Bool())
	val instNextReadyCache=RegInit(false.B)
	val instNextReady=Wire(Bool())
	readBaseReadyCache:=Mux(fullReady,false.B,readBaseReady)
	readNextReadyCache:=Mux(fullReady,false.B,readNextReady)
	writeBaseReadyCache:=Mux(fullReady,false.B,writeBaseReady)
	writeNextReadyCache:=Mux(fullReady,false.B,writeNextReady)
	instBaseReadyCache:=Mux(fullReady,false.B,instBaseReady)
	instNextReadyCache:=Mux(fullReady,false.B,instNextReady)
	printf(p"----[readycache] $readBaseReadyCache $readNextReadyCache $writeBaseReadyCache $writeNextReadyCache $instBaseReadyCache $instNextReadyCache\n")
	//determine current subtask
	val readBaseTodo=(!readBaseReadyCache)&readBaseNeeded
	val readNextTodo=(!readNextReadyCache)&readNextNeeded
	val writeBaseTodo=(!writeBaseReadyCache)&writeBaseNeeded
	val writeNextTodo=(!writeNextReadyCache)&writeNextNeeded
	val instBaseTodo=(!instBaseReadyCache)&instBaseNeeded
	val instNextTodo=(!instNextReadyCache)&instNextNeeded
	val readBaseCurrent=readBaseTodo
	val readNextCurrent=readNextTodo&(!readBaseCurrent)
	val writeBaseCurrent=writeBaseTodo&(!readNextCurrent)&(!readBaseCurrent)
	val writeNextCurrent=writeNextTodo&(!writeBaseCurrent)&(!readNextCurrent)&(!readBaseCurrent)
	val instBaseCurrent=instBaseTodo&(!writeNextCurrent)&(!writeBaseCurrent)&(!readNextCurrent)&(!readBaseCurrent)
	val instNextCurrent=instNextTodo&(!instBaseCurrent)&(!writeNextCurrent)&(!writeBaseCurrent)&(!readNextCurrent)&(!readBaseCurrent)
	printf(p"----[current] $readBaseCurrent $readNextCurrent $writeBaseCurrent $writeNextCurrent $instBaseCurrent $instNextCurrent\n")
	//set ready after complete
	readBaseReady:=readBaseReadyCache|(readBaseCurrent&io.raw.ready)
	readNextReady:=readNextReadyCache|(readNextCurrent&io.raw.ready)
	writeBaseReady:=writeBaseReadyCache|(writeBaseCurrent&io.raw.ready)
	writeNextReady:=writeNextReadyCache|(writeNextCurrent&io.raw.ready)
	instBaseReady:=instBaseReadyCache|(instBaseCurrent&io.raw.ready)
	instNextReady:=instNextReadyCache|(instNextCurrent&io.raw.ready)
	//determine fullReady
	val readBaseComplete=readBaseReady|(!readBaseNeeded)
	val readNextComplete=readNextReady|(!readNextNeeded)
	val writeBaseComplete=writeBaseReady|(!writeBaseNeeded)
	val writeNextComplete=writeNextReady|(!writeNextNeeded)
	val instBaseComplete=instBaseReady|(!instBaseNeeded)
	val instNextComplete=instNextReady|(!instNextNeeded)
	fullReady:=readBaseComplete&readNextComplete&writeBaseComplete&writeNextComplete&instBaseComplete&instNextComplete
	//as a workaround to chisel bug we put this here
	val irdataCache=RegInit(0.U(128.W))
	//set raw command
	io.raw.addr:=MuxCase(0.U(32.W),Seq(
		readBaseCurrent->baseRwaddr,
		readNextCurrent->nextRwaddr,
		writeBaseCurrent->baseRwaddr,
		writeNextCurrent->nextRwaddr,
		instBaseCurrent->baseIaddr,
		instNextCurrent->nextIaddr
	))
	io.raw.read:=readBaseCurrent|readNextCurrent|instBaseCurrent|instNextCurrent
	io.raw.write:=writeBaseCurrent|writeNextCurrent
	val wdataCache=Wire(UInt(64.W))
	io.raw.wdata:=MuxCase(0.U(32.W),Seq(
		writeBaseCurrent->wdataCache(31,0),
		writeNextCurrent->wdataCache(63,32)
	))
	//fetch inst/read result
	val idataCache=irdataCache(63,0)
	val rdataCache=irdataCache(127,64)
	val baseIdata=Mux(instBaseCurrent&io.raw.ready,io.raw.rdata,idataCache(31,0))
	val nextIdata=Mux(instNextCurrent&io.raw.ready,io.raw.rdata,idataCache(63,32))
	val baseRdata=Mux(readBaseCurrent&io.raw.ready,io.raw.rdata,rdataCache(31,0))
	val nextRdata=Mux(readNextCurrent&io.raw.ready,io.raw.rdata,rdataCache(63,32))
	val idata=Cat(nextIdata,baseIdata)
	io.core.idata:=MuxLookup(task.iaddr(1,0),0.U,Seq(
		0.U->idata(31,0),
		1.U->idata(39,8),
		2.U->idata(47,16),
		3.U->idata(55,24)
	))
	val rdata=Cat(nextRdata,baseRdata)
	val rdata32=MuxLookup(task.rwaddr(1,0),0.U,Seq(
		0.U->rdata(31,0),
		1.U->rdata(39,8),
		2.U->rdata(47,16),
		3.U->rdata(55,24)
	))
	io.core.rdata:=MuxLookup(task.rwwidth,0.U,Seq(
		W_B->Cat(Fill(24,0.U),rdata32(7,0)),
		W_H->Cat(Fill(16,0.U),rdata32(15,0)),
		W_W->rdata32
	))
	irdataCache:=Cat(rdata,idata)
	//setup write content
	val wdata=MuxLookup(task.rwaddr(1,0),0.U,Seq(
		0.U->Cat(Fill(32,0.U),task.wdata),
		1.U->Cat(Fill(24,0.U),task.wdata,Fill(8,0.U)),
		2.U->Cat(Fill(16,0.U),task.wdata,Fill(16,0.U)),
		3.U->Cat(Fill(8,0.U),task.wdata,Fill(24,0.U))
	))
	val wdataBaseMask=MuxLookup(task.rwwidth,0.U,Seq(
		W_B->Cat(Fill(24,0.U),Fill(8,1.U)),
		W_H->Cat(Fill(16,0.U),Fill(16,1.U)),
		W_W->Fill(32,1.U)
	))
	val wdataMask=MuxLookup(task.rwaddr(1,0),0.U,Seq(
		0.U->Cat(Fill(32,0.U),wdataBaseMask),
		1.U->Cat(Fill(24,0.U),wdataBaseMask,Fill(8,0.U)),
		2.U->Cat(Fill(16,0.U),wdataBaseMask,Fill(16,0.U)),
		3.U->Cat(Fill(8,0.U),wdataBaseMask,Fill(24,0.U))
	))
	wdataCache:=(wdataMask&wdata)|((~wdataMask)&rdata)
}