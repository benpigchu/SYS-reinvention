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
	val rwtype=Output(UInt(3.W))
}

class CoreIO extends Bundle{
	val mem=new MemIO
}

class Core extends Module{
	val NOP="b00000000000000000000000000010011".U(32.W)
	val io=IO(new CoreIO)
	//-------Declares
	//-------PC
	val pc=RegInit(0.U(32.W))
	val pcStall=False.B//TODO
	val nextPc=Mux(pcStall,pc,pc+4.U)
	//-------IF
	io.mem.iaddr:=Cat(0.U(2.W),pc)//we do not have mmu now
	//-------ID
	val idStall=False.B//TODO
	val idInst=RegInit(NOP)
	idInst:=Mux(idStall,idInst,io.mem.idata)
	val idPc=RegInit(0.U(32.W))
	idPc:=Mux(idStall,idPc,pc)
	val idRegA=idInst(19,15)
	val idRegB=idInst(24,20)
	val control=Module(new Control)
	control.io.inst:=idInst
	val idSignal=control.io.signal
	val idImm=ImmGen(idSignal.immType,idInst)
	val regfile=Module(new RegFile)
	regfile.io.readRegA:=idRegA
	regfile.io.readRegB:=idRegB
	//-------EX
	val exStall=False.B//TODO
	val exPc=RegInit(0.U(32.W))
	exPc:=Mux(exStall,exPc,idPc)
	val exInst=RegInit(NOP)
	exInst:=Mux(exStall,exInst,idInst)
	val exImm=RegInit(0.U(32.W))
	exImm:=Mux(exStall,exImm,idImm)
	val exSignal=RegInit(0.U.asTypeOf(new ControlSignals))
	exSignal:=Mux(exStall,exSignal,idSignal)
	val exDataA=RegInit(0.U(32.W))
	exDataA:=Mux(exStall,exDataA,regfile.io.readDataA)
	val exDataB=RegInit(0.U(32.W))
	exDataB:=Mux(exStall,exDataB,regfile.io.readDataB)
	val alu=Module(new ALU)
	alu.io.op:=exSignal.aluOp
	import ALUASourceSignal._
	alu.io.inputA:=MuxLookup(exSignal.aluASource,0.U,Seq(
		A_REGA->exDataA,
		A_PC->exPc
	))
	import ALUBSourceSignal._
	alu.io.inputB:=MuxLookup(exSignal.aluBSource,0.U,Seq(
		B_REGB->exDataB,
		B_IMM->exImm
	))
	//-------ME
	val meStall=False.B//TODO
	val mePc=RegInit(0.U(32.W))
	mePc:=Mux(meStall,mePc,exPc)
	val meInst=RegInit(NOP)
	meInst:=Mux(meStall,meInst,exInst)
	val meImm=RegInit(0.U(32.W))
	meImm:=Mux(meStall,meImm,exImm)
	val meSignal=RegInit(0.U.asTypeOf(new ControlSignals))
	meSignal:=Mux(meStall,meSignal,exSignal)
	val meDataA=RegInit(0.U(32.W))
	meDataA:=Mux(meStall,meDataA,exDataA)
	val meDataB=RegInit(0.U(32.W))
	meDataB:=Mux(meStall,meDataB,exDataB)
	val meALUResult=RegInit(0.U(32.W))
	meALUResult:=Mux(meStall,meALUResult,alu.io.output)
	//current nothing
	//-------WB
	val wbStall=False.B//TODO
	val wbPc=RegInit(0.U(32.W))
	wbPc:=Mux(wbStall,wbPc,mePc)
	val wbInst=RegInit(NOP)
	wbInst:=Mux(wbStall,wbInst,meInst)
	val wbImm=RegInit(0.U(32.W))
	wbImm:=Mux(wbStall,wbImm,meImm)
	val wbSignal=RegInit(0.U.asTypeOf(new ControlSignals))
	wbSignal:=Mux(wbStall,wbSignal,meSignal)
	val wbDataA=RegInit(0.U(32.W))
	wbDataA:=Mux(wbStall,wbDataA,meDataA)
	val wbDataB=RegInit(0.U(32.W))
	wbDataB:=Mux(wbStall,wbDataB,meDataB)
	val wbALUResult=RegInit(0.U(32.W))
	wbALUResult:=Mux(wbStall,wbALUResult,meALUResult)
	regfile.io.writeEnable:=wbSignal.writeBack
	val wbRegDest=wbInst(11,7)
	regfile.io.writeReg:=wbRegDest
	val wbRegData=wbALUResult
	regfile.io.writeData:=wbRegData
	//-------Control
}