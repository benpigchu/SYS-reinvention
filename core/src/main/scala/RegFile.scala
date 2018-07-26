package reinventation_core

import chisel3._
import chisel3.util._

class RegFileIO extends Bundle{
	val writeEnable=Input(Bool())
	val writeReg=Input(UInt(5.W))
	val readRegA=Input(UInt(5.W))
	val readRegB=Input(UInt(5.W))
	val writeData=Input(UInt(32.W))
	val readDataA=Output(UInt(32.W))
	val readDataB=Output(UInt(32.W))
	val debug=Output(Vec(32,UInt(32.W)))
}

class RegFile extends Module{
	val io=IO(new RegFileIO)
	val regs=Mem(32,UInt(32.W))
	io.readDataA:=Mux(io.readRegA=/=0.U,Mux(io.writeEnable&(io.readRegA===io.writeReg),io.writeData,regs(io.readRegA)),0.U)
	io.readDataB:=Mux(io.readRegB=/=0.U,Mux(io.writeEnable&(io.readRegB===io.writeReg),io.writeData,regs(io.readRegB)),0.U)
	when(io.writeEnable&(io.writeReg=/=0.U)){
		regs(io.writeReg):=io.writeData
	}
	for(i<-0 until 32){
		io.debug(i):=regs(i)
	}
}