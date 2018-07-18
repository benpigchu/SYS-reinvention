package reinventation_core

import chisel3._

class MemIO extends Bundle{
	val request=Output(Bool())
	val ready=Input(Bool())
	val iaddr=Output(UInt(34.W))
	val idata=Input(UInt(32.W))
	val read=Output(Bool())
	val write=Output(Bool())
	val rwaddr=Output(UInt(34.W))
	val rdata=Input(UInt(32.W))
	val wdata=Output(UInt(32.W))
	// TODO
}

class CoreIO extends Bundle{
	val mem=new MemIO
}

class Core extends Module{
	val io=IO(new CoreIO)
}