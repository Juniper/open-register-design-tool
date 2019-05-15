# filterregs.coffee - calls getDescendants with input rname 
#                      uses returned json to generate filterregs table
#
$ ->
  # ----- functions

  # create abbreviated category string
  createCatAbrString = (cat) ->
    result = ''
    if cat & 1 then result += ' SCFG' 
    if cat & 2 then result += ' DCFG' 
    if cat & 4 then result += ' CCFG' 
    if cat & 8 then result += ' SCTR' 
    if cat & 16 then result += ' ECTR' 
    if cat & 32 then result += ' ST' 
    if cat & 64 then result += ' INTR' 
    if cat & 128 then result += ' DIAG' 
    if cat & 256 then result += ' DBG' 
    if cat & 512 then result += ' CGU' 
    result
  
  # ----- create filter results table 
  filIdElems = $("#filterregs")
  cid = filIdElems.data('cid')
  scope = filIdElems.data('scope')
  dids = filIdElems.data('dids')
  rstype = filIdElems.data('rstype')
  rntype = filIdElems.data('rntype')
  namemode = filIdElems.data('namemode')
  rname = filIdElems.data('rname')
  rwidth = filIdElems.data('rwidth')
  raddr = filIdElems.data('raddr')
  unaddr = filIdElems.data('unaddr')
  rcat = filIdElems.data('rcat')
  dtest = filIdElems.data('dtest')
  fntype = filIdElems.data('fntype')
  fnamemode = filIdElems.data('fnamemode')
  fname = filIdElems.data('fname')
  faccess = filIdElems.data('faccess')
  fscat = filIdElems.data('fscat')
  fnoinit = filIdElems.data('fnoinit')
  fvol = filIdElems.data('fvol')
  fcount = filIdElems.data('fcount')
  fintr = filIdElems.data('fintr')
  fdcomp = filIdElems.data('fdcomp')
  #alert("rname=" + rname)
  rr = jsRoutes.controllers.Application.filterRegs(cid, scope, dids, rstype, rntype, namemode, rname, rwidth, raddr, unaddr, rcat, dtest, fntype, fnamemode, fname, faccess, fscat, fnoinit, fvol, fcount, fintr, fdcomp)
  #alert("type=" + rr.type + ", url=" + rr.url)
  $.get rr.url, (registers) ->
    # ---- update number of results returned
    frescountEl = $("#frescount").first()
    frescountEl.text(registers.length)
    #alert("results=" + registers.length)
    # ---- build the table 
    hrow = $("<tr>")
    hrow.addClass("active")
    hrow.append $("<th></th>")
    hrow.append $("<th>name</th>")
    hrow.append $("<th>base address (0x)</th>")
    hrow.append $("<th>width</th>")
    hrow.append $("<th>access</th>")
    hrow.append $("<th>reps</th>")
    hrow.append $("<th>stride (0x)</th>")
    hrow.append $("<th>dt</th>")
    hrow.append $("<th>cat</th>")
    hrow.append $("<th>short text</th>")
    hrow.append $("<th>data id</th>")
    hdr = $("<thead>").append hrow
    filIdElems.append hdr   
    bod = $("<tbody>")
    filIdElems.append bod   
    lastPPath = ''
    $.each registers, (index, register) ->
      # --- build parent path row
      #alert("1-name=" + register.name)
      pPath = register.parentPath
      if pPath? and pPath != lastPPath
         brow = $("<tr>")
         txt = '<td colspan="9" class="info">' + pPath + '</td>'
         brow.append $(txt)
         brow.append $("<td></td>") 
         bod.append brow
         lastPPath = pPath
      # -- build main row for this reg/regset
      brow = $("<tr>")
      #if register.isRegset then brow.addClass("info")
      txt = if register.isRegset then '&#9776;' else ""
      txt = ""
      brow.append $("<td>#{txt}</td>")
      if register.isRegset
         rr = jsRoutes.controllers.Application.regsetPage(register.id, register.depth, 1)
      else
         rr = jsRoutes.controllers.Application.registerPage(register.id)
      #alert("type=" + rr.type + ", url=" + rr.url)
      brow.append $("<td><a href=\"#{rr.url}\">#{register.name}</a></td>")
      brow.append $("<td>#{register.baseAddr.toString(16)}</td>")
      txt = if register.isRegset then "" else register.width
      brow.append $("<td>#{txt}</td>")
      txt = if register.isRegset then "" else register.access
      brow.append $("<td>#{txt}</td>")
      brow.append $("<td>#{register.reps}</td>")
      txt = if register.reps > 1 then register.stride.toString(16) else ""
      brow.append $("<td>#{txt}</td>") 
      txt = if register.donttest then 'y' else ''
      brow.append $("<td>#{txt}</td>") 
      brow.append $("<td>#{createCatAbrString(register.catCode)}</td>")
      txt = register.shortText ? ""
      brow.append $("<td>#{txt}</td>")
      brow.append $("<td>#{register.loadId}</td>") 
      bod.append brow    
