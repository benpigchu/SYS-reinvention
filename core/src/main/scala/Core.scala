package reinventation_core

import chisel3._
import chisel3.util._

class MemIO extends Bundle{
	val ready=Input(Bool())
	val iaddr=Output(UInt(34.W))
	val idata=Input(UInt(32.W))
	val access=Output(Bool())
	val rwtype=Output(UInt(MemOpSignal.width))
	val rwaddr=Output(UInt(34.W))
	val rdata=Input(UInt(32.W))
	val wdata=Output(UInt(32.W))
	val rwwidth=Output(UInt(MemWidthSignal.width))
}

class DebugIO extends Bundle{
	val regs=Output(Vec(32,UInt(32.W)))
}

class CoreIO extends Bundle{
	val mem=new MemIO
	val debug=new DebugIO
}

class Core extends Module{
	val NOP="b00000000000000000000000000010011".U(32.W)
	val io=IO(new CoreIO)
	//-------Declares
	//should we stall and keep the status in this stage?
	val pcStall=Wire(Bool())
	val idStall=Wire(Bool())
	val exStall=Wire(Bool())
	val meStall=Wire(Bool())
	val wbStall=Wire(Bool())
	//-------IF
	val pc=RegInit(0.U(32.W))
	pcStall:=idStall
	val nextPc=Mux(pcStall,pc,pc+4.U)
	pc:=nextPc
	io.mem.iaddr:=Cat(0.U(2.W),pc)//we do not have mmu now
	//-------ID
	idStall:=exStall//TODO
	val idInvalid=RegInit(false.B)
	idInvalid:=pcStall&(!idStall)
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
	//also calculate address for me
	exStall:=meStall//TODO
	val exInvalid=RegInit(false.B)
	exInvalid:=(idStall&(!exStall))|idInvalid
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
	meStall:=wbStall//TODO
	val meInvalid=RegInit(false.B)
	meInvalid:=(exStall&(!meStall))|exInvalid
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
	printf(p"----[me] inst ${Binary(meInst)} access ${meSignal.accessMem} type ${meSignal.memOp}")
	printf(p"----[me] imm ${Binary(meImm)}")
	io.mem.access:=(!meInvalid)&meSignal.accessMem
	io.mem.rwtype:=meSignal.memOp
	io.mem.rwaddr:=meALUResult
	io.mem.wdata:=meDataB
	io.mem.rwwidth:=meSignal.memWidth
	val meRawLoadedData=io.mem.rdata
	import MemWidthSignal._
	val meLoadedData=MuxLookup(meSignal.memWidth,0.U,Seq(
		W_W->meRawLoadedData,
		W_H->Cat(Fill(16,Mux(meSignal.memLoadUnsigned,0.U,meRawLoadedData(15))),meRawLoadedData(15,0)),
		W_B->Cat(Fill(24,Mux(meSignal.memLoadUnsigned,0.U,meRawLoadedData(7))),meRawLoadedData(7,0))
	))
	//-------WB
	wbStall:=(!io.mem.ready)//currently we block the whole pipeline when io is not ready
	val wbInvalid=RegInit(false.B)
	wbInvalid:=(meStall&(!wbStall))|meInvalid
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
	val wbLoadedData=RegInit(0.U(32.W))
	wbLoadedData:=Mux(wbStall,wbLoadedData,meLoadedData)
	regfile.io.writeEnable:=wbSignal.writeBack&(!wbInvalid)
	val wbRegDest=wbInst(11,7)
	regfile.io.writeReg:=wbRegDest
	import WritebackSourceSignal._
	val wbRegData=MuxLookup(wbSignal.writeBackSource,wbALUResult,Seq(
		WB_ALU->wbALUResult,
		WB_MEM->wbLoadedData
	))
	regfile.io.writeData:=wbRegData
	//-------Control
	//-------Debug
	io.debug.regs:=regfile.io.debug
	printf("----------------\n")
	printf(p"stall: pc=$pcStall id=$idStall ex=$exStall me=$meStall wb=$wbStall\n")
	printf(p"invalid: id=$idInvalid ex=$exInvalid me=$meInvalid wb=$wbInvalid\n")
	printf(p"pc: pc=0x${Hexadecimal(pc)} id=0x${Hexadecimal(idPc)} ex=0x${Hexadecimal(exPc)} me=0x${Hexadecimal(mePc)} wb=0x${Hexadecimal(wbPc)}\n")
}