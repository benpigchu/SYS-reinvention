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
	//regs
	val pc=RegInit(0.U(32.W))
	val idInvalid=RegInit(false.B)
	val idInst=RegInit(NOP)
	val idPc=RegInit(0.U(32.W))
	val exInvalid=RegInit(false.B)
	val exPc=RegInit(0.U(32.W))
	val exInst=RegInit(NOP)
	val exImm=RegInit(0.U(32.W))
	val exSignal=RegInit(0.U.asTypeOf(new ControlSignals))
	val exRawDataA=RegInit(0.U(32.W))
	val exRawDataB=RegInit(0.U(32.W))
	val meInvalid=RegInit(false.B)
	val mePc=RegInit(0.U(32.W))
	val meInst=RegInit(NOP)
	val meImm=RegInit(0.U(32.W))
	val meSignal=RegInit(0.U.asTypeOf(new ControlSignals))
	val meDataA=RegInit(0.U(32.W))
	val meDataB=RegInit(0.U(32.W))
	val meALUResult=RegInit(0.U(32.W))
	val wbInvalid=RegInit(false.B)
	val wbPc=RegInit(0.U(32.W))
	val wbInst=RegInit(NOP)
	val wbImm=RegInit(0.U(32.W))
	val wbSignal=RegInit(0.U.asTypeOf(new ControlSignals))
	val wbDataA=RegInit(0.U(32.W))
	val wbDataB=RegInit(0.U(32.W))
	val wbALUResult=RegInit(0.U(32.W))
	val wbLoadedData=RegInit(0.U(32.W))
	//should we stall and keep the status in this stage?
	val pcStall=Wire(Bool())
	val idStall=Wire(Bool())
	val exStall=Wire(Bool())
	val meStall=Wire(Bool())
	val wbStall=Wire(Bool())
	//-------IF
	pcStall:=idStall
	val nextPc=Mux(pcStall,pc,pc+4.U)
	pc:=nextPc
	io.mem.iaddr:=Cat(0.U(2.W),pc)//we do not have mmu now
	//-------ID
	idStall:=exStall//TODO
	idInvalid:=pcStall&(!idStall)
	idInst:=Mux(idStall,idInst,io.mem.idata)
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
	val useAfterLoadHazard=Wire(Bool())
	exStall:=meStall|useAfterLoadHazard//TODO
	exInvalid:=(idStall&(!exStall))|idInvalid
	exPc:=Mux(exStall,exPc,idPc)
	exInst:=Mux(exStall,exInst,idInst)
	exImm:=Mux(exStall,exImm,idImm)
	exSignal:=Mux(exStall,exSignal,idSignal)
	exRawDataA:=Mux(exStall,exRawDataA,regfile.io.readDataA)
	exRawDataB:=Mux(exStall,exRawDataB,regfile.io.readDataB)
	val exRegA=exInst(19,15)
	val exRegB=exInst(24,20)
	val exDataA=Wire(UInt(32.W))
	val exDataB=Wire(UInt(32.W))
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
	meInvalid:=(exStall&(!meStall))|exInvalid
	mePc:=Mux(meStall,mePc,exPc)
	meInst:=Mux(meStall,meInst,exInst)
	meImm:=Mux(meStall,meImm,exImm)
	meSignal:=Mux(meStall,meSignal,exSignal)
	meDataA:=Mux(meStall,meDataA,exDataA)
	meDataB:=Mux(meStall,meDataB,exDataB)
	meALUResult:=Mux(meStall,meALUResult,alu.io.output)
	val meRegDest=meInst(11,7)
	printf(p"----[me] inst ${Binary(meInst)} access ${meSignal.accessMem} type ${meSignal.memOp}\n")
	printf(p"----[me] imm ${Binary(meImm)}\n")
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
	wbInvalid:=(meStall&(!wbStall))|meInvalid
	wbPc:=Mux(wbStall,wbPc,mePc)
	wbInst:=Mux(wbStall,wbInst,meInst)
	wbImm:=Mux(wbStall,wbImm,meImm)
	wbSignal:=Mux(wbStall,wbSignal,meSignal)
	wbDataA:=Mux(wbStall,wbDataA,meDataA)
	wbDataB:=Mux(wbStall,wbDataB,meDataB)
	wbALUResult:=Mux(wbStall,wbALUResult,meALUResult)
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
	//-------Forward+Hazard
	val exNeedMeForwardA=exSignal.useRegA&(!meInvalid)&meSignal.writeBack&(meRegDest===exRegA)&(!meSignal.accessMem)
	val exNeedMeForwardB=exSignal.useRegB&(!meInvalid)&meSignal.writeBack&(meRegDest===exRegB)&(!meSignal.accessMem)
	val exNeedWbForwardA=exSignal.useRegA&(!wbInvalid)&wbSignal.writeBack&(wbRegDest===exRegA)
	val exNeedWbForwardB=exSignal.useRegB&(!wbInvalid)&wbSignal.writeBack&(wbRegDest===exRegB)
	exDataA:=Mux(exNeedMeForwardA,meALUResult,Mux(exNeedWbForwardA,wbRegData,exRawDataA))
	exDataB:=Mux(exNeedMeForwardB,meALUResult,Mux(exNeedWbForwardB,wbRegData,exRawDataB))
	printf(p"----[forword] regA:$exRegA dataA:${Hexadecimal(exDataA)} use:${exSignal.useRegA}\n")
	printf(p"----[forword] regB:$exRegA dataB:${Hexadecimal(exDataB)} use:${exSignal.useRegB}\n")
	printf(p"----[forword] me:$meRegDest dataB:${Hexadecimal(meALUResult)} use:${meSignal.writeBack}\n")
	printf(p"----[forword] wb:$wbRegDest dataB:${Hexadecimal(wbRegData)} use:${wbSignal.writeBack}\n")
	val useAfterLoadHazardA=exSignal.useRegA&(!meInvalid)&meSignal.writeBack&(meRegDest===exRegA)&(meSignal.accessMem)
	val useAfterLoadHazardB=exSignal.useRegA&(!meInvalid)&meSignal.writeBack&(meRegDest===exRegA)&(meSignal.accessMem)
	useAfterLoadHazard:=useAfterLoadHazardA|useAfterLoadHazardB
	//-------Debug
	io.debug.regs:=regfile.io.debug
	printf("----------------\n")
	printf(p"stall: pc=$pcStall id=$idStall ex=$exStall me=$meStall wb=$wbStall\n")
	printf(p"invalid: id=$idInvalid ex=$exInvalid me=$meInvalid wb=$wbInvalid\n")
	printf(p"pc: pc=0x${Hexadecimal(pc)} id=0x${Hexadecimal(idPc)} ex=0x${Hexadecimal(exPc)} me=0x${Hexadecimal(mePc)} wb=0x${Hexadecimal(wbPc)}\n")
}